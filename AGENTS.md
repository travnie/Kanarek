# AGENTS.md

Kanarek: Android reader/player and supporting Cloudflare Worker.

## Layout

- `app/`: Kotlin/Compose Android app, news widgets, radio/IPTV player, player widget.
- `worker/`: optional TypeScript Worker for feed proxying, discovery/scraping, synchronized state.
- Empty backend config: on-device feed parsing must stay functional.
- Generated/build output: not maintained source.

## Work

- Check `main`, open PRs and recent changes before overlapping work.
- Keep one maintained source of truth per concern.
- Never commit credentials, tokens or private deployment metadata. Non-secret
  Wrangler config (e.g. account, binding and resource IDs) may stay in
  `worker/wrangler.jsonc` when needed for reproducible deployment.
- `megalinter-reports/updated_sources`: suggestions; apply only intended fixes.
