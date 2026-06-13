#!/bin/sh
# SDRTrunk liveness healthcheck for systemd deployments.
#
# Polls the local REST /health endpoint and restarts the sdrtrunk unit only when the process is
# wedged: no HTTP response at all on two consecutive probes.  A 503 response means degraded but
# alive (e.g. a tuner in recovery) and is NOT a restart condition - the application self-heals
# those states.  A startup grace period avoids restarting an instance that is still initializing.
#
# Install (with sdrtrunk-healthcheck.service and .timer):
#   sudo cp deploy/sdrtrunk-healthcheck.* /etc/systemd/system/   # .service and .timer
#   sudo cp deploy/sdrtrunk-healthcheck.sh /usr/local/bin/ && sudo chmod +x /usr/local/bin/sdrtrunk-healthcheck.sh
#   sudo systemctl daemon-reload && sudo systemctl enable --now sdrtrunk-healthcheck.timer

PORT="${SDRTRUNK_API_PORT:-8080}"
UNIT="${SDRTRUNK_UNIT:-sdrtrunk}"
GRACE_SECONDS=300

systemctl is-active --quiet "$UNIT" || exit 0

# Startup grace period
STARTED=$(systemctl show "$UNIT" --property=ActiveEnterTimestampMonotonic --value)
NOW=$(awk '{printf "%d", $1 * 1000000}' /proc/uptime)
if [ -n "$STARTED" ] && [ "$STARTED" -gt 0 ] && [ $((NOW - STARTED)) -lt $((GRACE_SECONDS * 1000000)) ]; then
    exit 0
fi

probe() {
    # Any HTTP response (200 or 503) counts as alive; only connection failure/timeout fails
    curl -s -o /dev/null --max-time 10 "http://127.0.0.1:${PORT}/health"
}

if probe; then
    exit 0
fi

sleep 30

if probe; then
    exit 0
fi

logger -t sdrtrunk-healthcheck "No response from /health on port ${PORT} after two probes - restarting ${UNIT}"
systemctl restart "$UNIT"
