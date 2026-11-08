#!/usr/bin/env bash
# One-time setup on a fresh Ubuntu 22.04/24.04 EC2 instance.
# Usage: sudo bash ec2-bootstrap.sh api.example.com you@example.com
set -euo pipefail
DOMAIN="${1:?domain}"; EMAIL="${2:?email}"
APP_USER="${SUDO_USER:-ubuntu}"

# 2 GB swap (small instances run out of memory during image pulls/JVM start)
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

apt-get update
apt-get install -y ca-certificates curl nginx certbot python3-certbot-nginx ufw
curl -fsSL https://get.docker.com | sh
usermod -aG docker "$APP_USER"

mkdir -p "/home/$APP_USER/portfolio" && chown "$APP_USER":"$APP_USER" "/home/$APP_USER/portfolio"

# Nginx reverse proxy
sed "s/api.example.com/$DOMAIN/" "$(dirname "$0")/nginx-api.conf" > /etc/nginx/sites-available/portfolio-api
ln -sf /etc/nginx/sites-available/portfolio-api /etc/nginx/sites-enabled/portfolio-api
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

# Host firewall (the EC2 security group should also allow only 22/80/443)
ufw allow OpenSSH && ufw allow 'Nginx Full' && ufw --force enable

# HTTPS (DNS A record for $DOMAIN must already point at this instance)
certbot --nginx -d "$DOMAIN" -m "$EMAIL" --agree-tos --no-eff-email --redirect -n
echo "Done. Put .env in /home/$APP_USER/portfolio and run the deploy workflow."
