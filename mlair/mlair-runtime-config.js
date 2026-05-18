// Browser → MLAir API via mlair-browser-gateway (same origin).
// Compose (:38080): API at origin (/v1). EKS: ingress subpath → set HUB_API_PREFIX=/mlair in .j2.
(function () {
  var HUB_API_PREFIX = "";
  var USE_EKS_ROOT_HUB_PATHS = false;
  var HUB_ROOT_PATHS = [
    "/_next",
    "/mlair-runtime-config.js",
    "/dashboard",
    "/datasets",
    "/pipelines",
    "/runs",
    "/models",
    "/experiments",
    "/lineage",
    "/lifecycle",
    "/search",
    "/tasks",
    "/task",
    "/registry",
    "/tenants",
    "/projects",
    "/settings",
    "/login",
    "/admin",
    "/artifacts",
    "/monitoring",
    "/features",
    "/governance",
    "/deployments",
    "/serving",
    "/plugins",
    "/schedules",
    "/notifications",
    "/audit",
    "/workspaces",
  ];

  function hubApiBase() {
    var bare = window.location.origin;
    var path = window.location.pathname || "";
    var prefix = String(HUB_API_PREFIX || "").trim();
    if (prefix) return bare + prefix;
    if (path === "/mlair" || path.indexOf("/mlair/") === 0) return bare + "/mlair";
    if (USE_EKS_ROOT_HUB_PATHS) {
      for (var i = 0; i < HUB_ROOT_PATHS.length; i++) {
        var p = HUB_ROOT_PATHS[i];
        if (path === p || path.indexOf(p + "/") === 0) return bare + "/mlair";
      }
    }
    return bare;
  }

  function needsMlairPrefix() {
    return hubApiBase() !== window.location.origin;
  }

  function isBadApiBase(url) {
    if (!url) return true;
    var expected = hubApiBase();
    if (url === expected) return false;
    var bare = window.location.origin;
    if (!needsMlairPrefix() && (url === bare + "/mlair" || url.indexOf(bare + "/mlair/") === 0)) return true;
    if (needsMlairPrefix() && (url === bare || url === bare + "/")) return true;
    return /:(8080|18080)(\/|$)/.test(url);
  }

  function applyOriginConfig() {
    var cfg = window.__ML_AIR_RUNTIME_CONFIG__ || {};
    var expected = hubApiBase();
    var api = String(cfg.api_base_url || cfg.apiBaseUrl || "").trim();
    if (!api || isBadApiBase(api) || api !== expected) {
      window.__ML_AIR_RUNTIME_CONFIG__ = {
        ...cfg,
        environment: cfg.environment || "docker-compose",
        api_base_url: expected,
        apiBaseUrl: expected,
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
    api_base_url: hubApiBase(),
    apiBaseUrl: hubApiBase(),
    realtime_base_url: "",
    default_tenant_hint: "default",
    default_project_hint: "default_project",
    features: {},
  };

  window.addEventListener("mlair-runtime-config-updated", applyOriginConfig);
  applyOriginConfig();
})();
