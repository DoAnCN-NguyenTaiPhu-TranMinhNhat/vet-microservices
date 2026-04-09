FROM grafana/grafana:5.2.4
RUN grafana-cli plugins install grafana-piechart-panel
ADD ./provisioning /etc/grafana/provisioning
ADD ./grafana.ini /etc/grafana/grafana.ini
ADD ./dashboards /var/lib/grafana/dashboards
