import { corsHeaders } from "./cors.ts";

export function htmlPage(title: string, body: string): Response {
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${title}</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 32rem; margin: 3rem auto; padding: 0 1rem; line-height: 1.5; }
    h1 { font-size: 1.35rem; }
    p { color: #444; }
    a.btn { display: inline-block; margin-top: 1rem; padding: 0.75rem 1rem; background: #ff6a00; color: #fff;
      border-radius: 8px; text-decoration: none; font-weight: 700; }
    .muted { font-size: 0.9rem; color: #666; }
  </style>
</head>
<body>
  <h1>${title}</h1>
  ${body}
</body>
</html>`;
  return new Response(html, {
    status: 200,
    headers: { ...corsHeaders, "Content-Type": "text/html; charset=utf-8" },
  });
}

type PaymentBridgeOptions = {
  title: string;
  message: string;
  deeplink: string;
  buttonLabel: string;
  footnote: string;
};

export function paymentBridgePage(options: PaymentBridgeOptions): Response {
  const deeplink = escapeHtmlAttr(options.deeplink);
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${escapeHtml(options.title)}</title>
  <meta http-equiv="refresh" content="0;url=${deeplink}" />
  <style>
    body { font-family: system-ui, sans-serif; max-width: 32rem; margin: 3rem auto; padding: 0 1rem; line-height: 1.5; }
    h1 { font-size: 1.35rem; }
    p { color: #444; }
    a.btn { display: inline-block; margin-top: 1rem; padding: 0.75rem 1rem; background: #ff6a00; color: #fff;
      border-radius: 8px; text-decoration: none; font-weight: 700; }
    .muted { font-size: 0.9rem; color: #666; margin-top: 1.25rem; }
  </style>
</head>
<body>
  <h1>${escapeHtml(options.title)}</h1>
  <p>${escapeHtml(options.message)}</p>
  <p><a class="btn" href="${deeplink}">${escapeHtml(options.buttonLabel)}</a></p>
  <p class="muted">${escapeHtml(options.footnote)}</p>
  <script>
    window.location.replace(${JSON.stringify(options.deeplink)});
  </script>
</body>
</html>`;
  return new Response(html, {
    status: 200,
    headers: { ...corsHeaders, "Content-Type": "text/html; charset=utf-8" },
  });
}

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function escapeHtmlAttr(value: string): string {
  return escapeHtml(value);
}
