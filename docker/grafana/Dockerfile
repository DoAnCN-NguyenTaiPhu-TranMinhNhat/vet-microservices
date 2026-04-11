# Grafana ≥8 cần cho dashboard JSON dùng panel `timeseries` / `piechart` (vet-ai-monitoring-dashboard.json).
FROM grafana/grafana:10.4.15
ADD ./provisioning /etc/grafana/provisioning
ADD ./grafana.ini /etc/grafana/grafana.ini
ADD ./dashboards /var/lib/grafana/dashboards
