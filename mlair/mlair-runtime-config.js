// Browser → MLAir API. Use the same origin as the page so uploads hit /v1 via mlair-browser-gateway
// (nginx proxies /v1 → mlair-api). Avoids NetworkError when MLAIR_API_HOST_PORT is blocked from the
// browser while the UI port is open, and avoids http/https mixed-origin issues.
(function () {
  window.__ML_AIR_RUNTIME_CONFIG__ = {
    environment: "docker-compose",
    api_base_url: window.location.origin,
    realtime_base_url: "",
    default_tenant_hint: "default",
    default_project_hint: "default_project",
    features: {}
  };
})();
