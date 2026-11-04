// IVAC OTP relay — Cloudflare Worker + one KV namespace (binding name: OTPS).
// Phone app  -> POST /push {key, code}
// Extension  -> POST /poll {key, after}
// Stores only the latest code per client key, for 5 minutes. The key is hashed before use.

const KEY_RE = /^[A-HJ-NP-Z2-9]{12}$/;
const enc = new TextEncoder();

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
};
const json = (o, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { 'Content-Type': 'application/json', ...CORS } });

async function slot(rawKey) {
  const k = String(rawKey || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (!KEY_RE.test(k)) return null;
  const d = await crypto.subtle.digest('SHA-256', enc.encode(k));
  return 'otp:' + [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

export default {
  async fetch(req, env) {
    if (req.method === 'OPTIONS') return new Response(null, { headers: CORS });
    if (req.method !== 'POST') return json({ ok: false, error: 'POST only' }, 405);
    const url = new URL(req.url);
    let body;
    try { body = await req.json(); } catch { return json({ ok: false, error: 'bad json' }, 400); }
    const id = await slot(body.key);
    if (!id) return json({ ok: false, error: 'bad key' }, 400);

    if (url.pathname === '/push') {
      const code = String(body.code || '');
      if (!/^\d{4,8}$/.test(code)) return json({ ok: false, error: 'bad code' }, 400);
      const rec = { id: crypto.randomUUID(), code, ts: Date.now() };
      await env.OTPS.put(id, JSON.stringify(rec), { expirationTtl: 300 });
      return json({ ok: true });
    }

    if (url.pathname === '/poll') {
      const raw = await env.OTPS.get(id);
      if (!raw) return json({ ok: true, empty: true });
      const rec = JSON.parse(raw);
      if (rec.id === body.after) return json({ ok: true, empty: true });
      return json({ ok: true, ...rec });
    }
    return json({ ok: false, error: 'not found' }, 404);
  },
};
