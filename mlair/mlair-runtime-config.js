// Browser → MLAir API via mlair-browser-gateway (same origin: :38080).
// Prevents NetworkError when Hub falls back to build-time http://localhost:8080 or stale Settings overrides.
(function () {
  var origin = window.location.origin;

  function isBadApiBase(url) {
    if (!url) return true;
    if (url === origin) return false;
    // Published API ports that the browser must not call directly when UI is on the gateway.
    return /:(8080|18080)(\/|$)/.test(url);
  }

  function applyOriginConfig() {
    var cfg = window.__ML_AIR_RUNTIME_CONFIG__ || {};
    var api = String(cfg.api_base_url || cfg.apiBaseUrl || "").trim();
    if (isBadApiBase(api)) {
      window.__ML_AIR_RUNTIME_CONFIG__ = {
        ...cfg,
        environment: cfg.environment || "docker-compose",
        api_base_url: origin,
        apiBaseUrl: origin,
        realtime_base_url: cfg.realtime_base_url || "",
        default_tenant_hint: cfg.default_tenant_hint || "default",
        default_project_hint: cfg.default_project_hint || "default_project",
        features: cfg.features || {},
      };
    }
  }

  try {
    var key = "mlair.runtime-config.override";
    var raw = localStorage.getItem(key);
    if (raw) {
      var o = JSON.parse(raw);
      var saved = String((o && o.apiBaseUrl) || "").trim();
      if (isBadApiBase(saved)) {
        delete o.apiBaseUrl;
        if (o && !o.jaegerBaseUrl) localStorage.removeItem(key);
        else localStorage.setItem(key, JSON.stringify(o));
      }
    }
  } catch (_) {
    /* ignore */
  }

  window.__ML_AIR_RUNTIME_CONFIG__ = {
    environment: "docker-compose",
    api_base_url: origin,
    apiBaseUrl: origin,
    realtime_base_url: "",
    default_tenant_hint: "default",
    default_project_hint: "default_project",
    features: {},
  };

  window.addEventListener("mlair-runtime-config-updated", applyOriginConfig);
  applyOriginConfig();
})();
