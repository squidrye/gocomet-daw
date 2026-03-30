#!/bin/bash
set -e

echo "=== GoComet Ride-Hailing Setup ==="
echo ""

# Check Java
if ! command -v java &> /dev/null; then
  echo "Java 21 is required. Install from https://adoptium.net/"
  exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 21 ]; then
  echo "Java 21+ required, found Java $JAVA_VER"
  exit 1
fi
echo "[ok] Java $JAVA_VER"

# Check PostgreSQL
if ! command -v psql &> /dev/null; then
  echo "PostgreSQL is required. Install from https://www.postgresql.org/download/"
  exit 1
fi
echo "[ok] PostgreSQL"

# Check Redis
if ! command -v redis-cli &> /dev/null; then
  echo "Redis is required. Install: brew install redis"
  exit 1
fi
echo "[ok] Redis"

# Check Node.js
if ! command -v node &> /dev/null; then
  echo "Node.js 18+ is required. Install from https://nodejs.org/"
  exit 1
fi
echo "[ok] Node.js $(node -v)"

# Setup database
echo ""
echo "Setting up PostgreSQL database..."
psql -U postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='ridehailing_api'" | grep -q 1 || \
  psql -U postgres -c "CREATE ROLE ridehailing_api SUPERUSER LOGIN PASSWORD 'ridehailing123';"
psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='ridehailingdb'" | grep -q 1 || \
  psql -U postgres -c "CREATE DATABASE ridehailingdb OWNER ridehailing_api;"
echo "[ok] Database ready"

# Ensure Redis is running
if ! redis-cli ping &> /dev/null; then
  echo "Starting Redis..."
  redis-server --daemonize yes
fi
echo "[ok] Redis running"

# Build backend
echo ""
echo "Building backend..."
./mvnw clean package -DskipTests -q
echo "[ok] Backend built"

# Install frontend deps
echo ""
echo "Installing frontend dependencies..."
cd ../core-web
npm install --silent
cd ../core-api

echo ""
echo "=== Setup Complete ==="
echo ""
echo "To run:"
echo "  Backend:  cd core-api && ./mvnw spring-boot:run"
echo "  Frontend: cd core-web && npm run dev"
echo ""
echo "  Backend runs on http://localhost:9000/v1"
echo "  Frontend runs on http://localhost:5173"
