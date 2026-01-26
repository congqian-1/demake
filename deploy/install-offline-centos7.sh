#!/usr/bin/env bash
set -euo pipefail

# Offline install for CentOS 7.9: JDK8 + MySQL 5.7
# Usage:
#   sudo bash install-offline-centos7.sh /path/to/rpms
#
# The rpm directory should contain:
# - java-1.8.0-openjdk*.rpm
# - java-1.8.0-openjdk-devel*.rpm
# - mysql57-community-release-el7-*.rpm
# - mysql-community-server*.rpm
# - mysql-community-client*.rpm
# - mysql-community-common*.rpm
# - mysql-community-libs*.rpm
# - mysql-community-libs-compat*.rpm

if [[ "${EUID}" -ne 0 ]]; then
  echo "[ERROR] Please run as root (sudo)."
  exit 1
fi

if [[ $# -lt 1 ]]; then
  echo "[ERROR] Missing rpm directory."
  echo "Usage: sudo bash $0 /path/to/rpms"
  exit 1
fi

RPM_DIR="$1"

if [[ ! -d "$RPM_DIR" ]]; then
  echo "[ERROR] RPM directory not found: $RPM_DIR"
  exit 1
fi

echo "[INFO] Using RPM directory: $RPM_DIR"

# 1) Install JDK 8
if ! java -version >/dev/null 2>&1; then
  echo "[INFO] Installing OpenJDK 8..."
  yum -y --disablerepo='*' localinstall \
    "$RPM_DIR"/java-1.8.0-openjdk*.rpm \
    "$RPM_DIR"/java-1.8.0-openjdk-devel*.rpm
else
  echo "[INFO] Java already installed."
fi

# 2) Install MySQL 5.7
if ! command -v mysqld >/dev/null 2>&1; then
  echo "[INFO] Installing MySQL 5.7..."
  yum -y --disablerepo='*' localinstall \
    "$RPM_DIR"/mysql57-community-release-el7-*.rpm || true

  yum -y --disablerepo='*' localinstall \
    "$RPM_DIR"/mysql-community-common*.rpm \
    "$RPM_DIR"/mysql-community-libs*.rpm \
    "$RPM_DIR"/mysql-community-libs-compat*.rpm \
    "$RPM_DIR"/mysql-community-client*.rpm \
    "$RPM_DIR"/mysql-community-server*.rpm
else
  echo "[INFO] MySQL already installed."
fi

# 3) Enable and start MySQL
systemctl enable --now mysqld

echo "[INFO] MySQL started."

# 4) Show temporary root password
if [[ -f /var/log/mysqld.log ]]; then
  echo "[INFO] Temporary MySQL root password:"
  grep 'temporary password' /var/log/mysqld.log || true
fi

echo "[DONE] Offline install completed."
