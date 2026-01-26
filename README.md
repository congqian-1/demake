# MES Service1 一键部署与升级（生产可用）

本文档只讲部署与升级，适合直接交付客户现场使用。

适用脚本：
- 一键部署：`deploy/ha/one-click-deploy.sh`
- 一键升级（失败自动回滚+告警）：`deploy/ha/upgrade.sh`

注意：
- 第三方 MES 地址请在 `mes-service1/src/main/resources/application.yml` 中配置。
- 一键部署脚本不再强制要求第三方地址参数。

## 1. 服务器准备
建议环境：
- Linux（systemd）
- JDK 8
- 能访问数据库与第三方 MES

检查：
```bash
java -version
```

如未安装（Ubuntu/Debian）：
```bash
sudo apt-get update -y
sudo apt-get install -y openjdk-8-jdk
```

如未安装（CentOS/RHEL）：
```bash
sudo yum install -y java-1.8.0-openjdk-devel
```

如需在 Linux 上安装 MySQL（按发行版二选一）：

Ubuntu/Debian：
```bash
sudo apt-get update -y
sudo apt-get install -y mysql-server
sudo systemctl enable --now mysql
```

CentOS/RHEL（8/9）：
```bash
sudo dnf install -y mysql-server
sudo systemctl enable --now mysqld
```

CentOS 7.9（MySQL 5.7 官方仓库）：
```bash
sudo yum install -y https://repo.mysql.com/mysql57-community-release-el7-11.noarch.rpm
sudo yum install -y mysql-community-server
sudo systemctl enable --now mysqld
```

初始化 root 密码与建库（CentOS 7.9）：
```bash
# 1) 查看初始 root 密码
sudo grep 'temporary password' /var/log/mysqld.log

# 2) 登录并修改 root 密码（临时密码默认过期，需加参数）
mysql --connect-expired-password -uroot -p

# 进入 MySQL 后执行（可把临时密码设为永久密码）：
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的新密码';
FLUSH PRIVILEGES;

# 3) （可选）创建数据库和用户
CREATE DATABASE mes DEFAULT CHARACTER SET utf8mb4;
CREATE USER 'mes'@'%' IDENTIFIED BY 'mes_password';
GRANT ALL PRIVILEGES ON mes.* TO 'mes'@'%';
FLUSH PRIVILEGES;
```

导入初始化表结构：
```bash
# 先确保数据库已创建
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS mes DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 初始化表结构（SQL 文件路径见下）
mysql -uroot -p -D mes < /opt/mes-release/init-mes.sql
```

初始化 SQL 文件：
- `deploy/ha/init-mes.sql`

## 2. 打包 Jar（在你的机器或CI执行）
```bash
mvn -pl mes-service1 -DskipTests package
```
产物：
- `mes-service1/target/mes-service1-1.0.0-SNAPSHOT.jar`

## 3. 推荐发布目录结构（服务器上）
把 Jar 和两个脚本放到同一目录，例如：

```text
/opt/mes-release/
  mes-service1-1.0.0-SNAPSHOT.jar
  one-click-deploy.sh
  upgrade.sh
```

示例命令：
```bash
sudo mkdir -p /opt/mes-release
# 上传后：
cd /opt/mes-release
ls
```

## 4. 一键部署（首次）
在发布目录执行：
```bash
cd /opt/mes-release
sudo bash one-click-deploy.sh ./mes-service1-1.0.0-SNAPSHOT.jar
```

部署完成后会自动：
- 注册 systemd 服务（开机自启）
- 服务异常自动重启
- 每分钟健康检查（异常自动重启+告警邮件）
- 日志按天/按大小切割并保留 30 天

## 5. 一键升级（后续只换 Jar）
把新版本 Jar 覆盖 `mes-service1-1.0.0-SNAPSHOT.jar`，然后执行：
```bash
cd /opt/mes-release
sudo bash upgrade.sh ./mes-service1-1.0.0-SNAPSHOT.jar
```

升级脚本会自动：
- 备份旧版本到 `/opt/mes-service1/backup/`
- 重启服务
- 健康检查失败自动回滚
- 回滚后自动发告警邮件

## 6. 部署结果检查（强烈建议）
查看服务状态：
```bash
sudo systemctl status mes-service1 --no-pager
```

查看健康检查：
```bash
curl -i http://127.0.0.1:8080/actuator/health
```

查看日志：
```bash
tail -n 200 /logs/mes-service1/mes-service1.log
```

## 7. 常用运维命令
重启：
```bash
sudo systemctl restart mes-service1
```

停止：
```bash
sudo systemctl stop mes-service1
```

开机自启：
```bash
sudo systemctl enable mes-service1
```

查看 systemd 日志：
```bash
sudo journalctl -u mes-service1 -n 200 --no-pager
```

## 8. 关键落地位置（部署后）
- 服务目录：`/opt/mes-service1`
- 配置目录：`/etc/mes-service1`
- 日志目录：`/logs/mes-service1`
- 日志切割：`/etc/logrotate.d/mes-service1`

## 9. 说明（与告警相关）
脚本已内置告警邮箱配置：
- 账号：`243219169@qq.com`
- 告警收件人：`243219169@qq.com`

如果客户现场禁外网或禁 SMTP，需要提前确认邮件能发出。
