/** Stateful edge endpoint guards; keep network input and pairing attempts bounded. */

export function securePairCode(alphabet: string, random?: () => number): string {
  const values = random ? null : crypto.getRandomValues(new Uint32Array(6));
  let code = "";
  for (let i = 0; i < 6; i++) {
    const sample = random ? random() : values![i] / 0x1_0000_0000;
    code += alphabet[Math.floor(sample * alphabet.length)];
  }
  return code;
}

export async function readLimitedJson(req: Request, maxBytes: number): Promise<unknown> {
  const advertised = Number(req.headers.get("content-length") || 0);
  if (advertised > maxBytes) throw new Error("payload too large");
  const reader = req.body?.getReader();
  if (!reader) return {};
  const decoder = new TextDecoder();
  let raw = "";
  let size = 0;
  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      if (!value) continue;
      size += value.byteLength;
      if (size > maxBytes) {
        await reader.cancel();
        throw new Error("payload too large");
      }
      raw += decoder.decode(value, { stream: true });
    }
    raw += decoder.decode();
  } finally {
    reader.releaseLock();
  }
  return raw ? JSON.parse(raw) : {};
}

/** Atomic per-IP D1 rate limit, hash the network identifier before storage. */
export async function allowPairClaim(
  req: Request, db: D1Database, windowMs: number, maxAttempts: number,
): Promise<boolean> {
  const address = req.headers.get("cf-connecting-ip") || "unknown-client";
  const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(address));
  const key = Array.from(new Uint8Array(hash), (n) => n.toString(16).padStart(2, "0")).join("");
  const window = Math.floor(Date.now() / windowMs);
  const result = await db.prepare(
    "INSERT INTO pair_claim_limits (client_key, window_bucket, attempts) VALUES (?1, ?2, 1) " +
    "ON CONFLICT(client_key) DO UPDATE SET window_bucket = excluded.window_bucket, " +
    "attempts = CASE WHEN pair_claim_limits.window_bucket = excluded.window_bucket THEN pair_claim_limits.attempts + 1 ELSE 1 END " +
    "WHERE pair_claim_limits.window_bucket != excluded.window_bucket OR pair_claim_limits.attempts < ?3 RETURNING attempts"
  ).bind(key, window, maxAttempts).first<{ attempts: number }>();
  return result !== null;
}
