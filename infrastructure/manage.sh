#!/bin/bash
# Infrastructure management script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

case "$1" in
    start)
        echo "Starting infrastructure services..."
        docker compose -f "$COMPOSE_FILE" up -d
        echo "Services started. Access MQTT UI at http://localhost:8080"
        ;;
    stop)
        echo "Stopping infrastructure services..."
        docker compose -f "$COMPOSE_FILE" down
        ;;
    restart)
        echo "Restarting infrastructure services..."
        docker compose -f "$COMPOSE_FILE" restart
        ;;
    logs)
        docker compose -f "$COMPOSE_FILE" logs -f "${@:2}"
        ;;
    ps|status)
        docker compose -f "$COMPOSE_FILE" ps
        ;;
    build)
        echo "Rebuilding services..."
        docker compose -f "$COMPOSE_FILE" build
        ;;
    clean)
        echo "Cleaning up (removing volumes)..."
        docker compose -f "$COMPOSE_FILE" down -v
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|logs|ps|status|build|clean}"
        echo ""
        echo "Commands:"
        echo "  start    - Start all services"
        echo "  stop     - Stop all services"
        echo "  restart  - Restart all services"
        echo "  logs     - View logs (add service name for specific service)"
        echo "  ps       - Show service status"
        echo "  status   - Show service status"
        echo "  build    - Rebuild all images"
        echo "  clean    - Stop and remove all volumes"
        echo ""
        echo "Examples:"
        echo "  $0 start"
        echo "  $0 logs simulator"
        echo "  $0 logs -f"
        exit 1
        ;;
esac
