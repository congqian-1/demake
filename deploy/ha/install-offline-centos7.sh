#!/usr/bin/env bash
set -euo pipefail

# Install for CentOS 7.9: JDK8 + MySQL 5.7
# Online (default):
#   sudo bash install-offline-centos7.sh
#   sudo bash install-offline-centos7.sh --online
#
# Offline:
#   sudo bash install-offline-centos7.sh --offline /path/to/rpms
#
# Offline RPM directory should contain:
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

MODE="online"
RPM_DIR=""

if [[ $# -ge 1 ]]; then
  case "$1" in
    --online)
      MODE="online"
      ;;
    --offline)
      MODE="offline"
      RPM_DIR="${2:-}"
      ;;
    *)
      echo "[ERROR] Unknown argument: $1"
      echo "Usage:"
      echo "  sudo bash $0"
      echo "  sudo bash $0 --online"
      echo "  sudo bash $0 --offline /path/to/rpms"
      exit 1
      ;;
  esac
fi

if [[ "$MODE" == "offline" ]]; then
  if [[ -z "$RPM_DIR" ]]; then
    echo "[ERROR] Missing rpm directory."
    echo "Usage: sudo bash $0 --offline /path/to/rpms"
    exit 1
  fi

  if [[ ! -d "$RPM_DIR" ]]; then
    echo "[ERROR] RPM directory not found: $RPM_DIR"
    exit 1
  fi

  echo "[INFO] Offline mode. Using RPM directory: $RPM_DIR"
else
  echo "[INFO] Online mode. Will install from internet repositories."
fi

configure_centos_repo_aliyun() {
  echo "[INFO] Configuring CentOS base repo to Aliyun mirrors..."
  if [[ -f /etc/yum.repos.d/CentOS-Base.repo ]]; then
    cp -a /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.bak.$(date +%Y%m%d%H%M%S)
  fi
  cat >/etc/yum.repos.d/CentOS-Base.repo <<'EOF'
[base]
name=CentOS-7 - Base - mirrors.aliyun.com
baseurl=http://mirrors.aliyun.com/centos/7/os/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
enabled=1

[updates]
name=CentOS-7 - Updates - mirrors.aliyun.com
baseurl=http://mirrors.aliyun.com/centos/7/updates/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
enabled=1

[extras]
name=CentOS-7 - Extras - mirrors.aliyun.com
baseurl=http://mirrors.aliyun.com/centos/7/extras/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
enabled=1
EOF
  yum clean all
  yum makecache fast || true
}

configure_mysql57_repo_aliyun() {
  echo "[INFO] Configuring MySQL 5.7 repo to Aliyun mirrors..."
  cat >/etc/yum.repos.d/mysql57-community.repo <<'EOF'
[mysql57-community]
name=MySQL 5.7 Community Server
baseurl=https://mirrors.aliyun.com/mysql/yum/mysql-5.7-community/el/7/$basearch/
enabled=1
gpgcheck=1
gpgkey=https://repo.mysql.com/RPM-GPG-KEY-mysql
EOF
  yum clean all
  yum makecache fast || true
}

ensure_mysql_gpg_key() {
  if ! rpm -q gpg-pubkey >/dev/null 2>&1 || ! rpm -q gpg-pubkey | grep -qi mysql; then
    echo "[INFO] Importing MySQL GPG key..."
    rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql || true
  fi
}

if [[ "$MODE" == "online" ]]; then
  if ! yum -q makecache fast; then
    echo "[WARN] Yum base repo unavailable. Switching to Aliyun mirror."
    configure_centos_repo_aliyun
  fi
fi

# 1) Install JDK 8
if ! java -version >/dev/null 2>&1; then
  echo "[INFO] Installing OpenJDK 8..."
  if [[ "$MODE" == "offline" ]]; then
    yum -y --disablerepo='*' localinstall \
      "$RPM_DIR"/java-1.8.0-openjdk*.rpm \
      "$RPM_DIR"/java-1.8.0-openjdk-devel*.rpm
  else
    yum -y install java-1.8.0-openjdk java-1.8.0-openjdk-devel
  fi
else
  echo "[INFO] Java already installed."
fi

# 2) Install MySQL 5.7
if ! command -v mysqld >/dev/null 2>&1; then
  echo "[INFO] Installing MySQL 5.7..."
  if [[ "$MODE" == "offline" ]]; then
    yum -y --disablerepo='*' localinstall \
      "$RPM_DIR"/mysql57-community-release-el7-*.rpm || true

    yum -y --disablerepo='*' localinstall \
      "$RPM_DIR"/mysql-community-common*.rpm \
      "$RPM_DIR"/mysql-community-libs*.rpm \
      "$RPM_DIR"/mysql-community-libs-compat*.rpm \
      "$RPM_DIR"/mysql-community-client*.rpm \
      "$RPM_DIR"/mysql-community-server*.rpm
  else
    if ! rpm -qa | grep -q mysql57-community-release; then
      if ! yum -y install https://repo.mysql.com/mysql57-community-release-el7-11.noarch.rpm; then
        echo "[WARN] Failed to reach repo.mysql.com. Using Aliyun MySQL mirror."
        configure_mysql57_repo_aliyun
      fi
    fi

    if ! command -v yum-config-manager >/dev/null 2>&1; then
      yum -y install yum-utils
    fi

    yum-config-manager --disable mysql80-community >/dev/null 2>&1 || true
    yum-config-manager --enable mysql57-community >/dev/null 2>&1 || true

    ensure_mysql_gpg_key
    if ! yum -y install mysql-community-server mysql-community-client; then
      echo "[WARN] MySQL install failed due to GPG. Retrying with --nogpgcheck..."
      yum -y --nogpgcheck install mysql-community-server mysql-community-client
    fi
  fi
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
