# LanCamera

网络摄像头 (LanCamera) 是一款专为局域网监控开发的一款简易 Android 应用，主要支持 `rtsp` 直播媒体流播放，同时支持经过 `nginx` 托管的录像文件播放。

## 说明

作者使用的是 萤石C3C 摄像头，一个 `openwrt` 路由器，同时外接一块硬盘。

**1\. 准备摄像头媒体流**

注意此部分内容因硬件配置差异，如果出现不能访问媒体流的问题，请务必优先 Google 搜索解决。

可以通过 萤石云 修改摄像头的用户名和密码参数，如增加 `test:123456` 这样的用户名和密码。再设置摄像头连接到路由器的无线网络。可以在路由器中查看摄像头的 ip 地址，并使用 `vlc` `ffpaly` 或 `ffmpeg` 等软件测试是否可以播放类似 `rtsp://test:123456@192.168.4.158:554` 的地址的视频流。注意 ip 地址为您的摄像头获取到的 ip。

**2\. 历史录像服务配置**

路由器安装 `nginx`

```shell
opkg update
opkg install nginx
```

注意，您可以使用 `nginx` 监听 80 端口来代理 openwrt 管理页面，也可以监听其他端口。将 `uhttpd` 监听修改为 `81`，并使用 `nginx` 代理。

```shell
    root /www;
    index index.html index.htm index.php;

    location / {
        try_files $uri $uri/ =404;
    }

    location /cgi-bin/luci {
        proxy_pass http://127.0.0.1:81;
    }

```

将硬盘挂载在 `/mnt/usb` 目录，同时使用了 `http basic auth` 来验证用户，避免被公开访问。注意如果您没有编译 `nginx-dav-ext-module` 模块，请移除配置中 `dav` 的相关配置。下边配置中 `/usb` `/usb2` 目录都可以通过浏览器访问，其中 `/usb2` 目录将以 `json` 格式返回数据并被应用解析。其中的 `.videopasswd` 文件可以通过 `htpasswd` 命令生成。
 
```shell
    location /usb {
        root /mnt/;
        auth_basic "Authentication required";
        auth_basic_user_file /etc/nginx/.videopasswd;
        autoindex_exact_size off;
        autoindex on;
        autoindex_localtime on;

        dav_methods PUT DELETE MKCOL COPY MOVE; 
        dav_ext_methods PROPFIND OPTIONS; 
        dav_access group:rw all:rw;
    }

    location /usb2 {
        alias /mnt/usb;
        auth_basic "Authentication required";
        auth_basic_user_file /etc/nginx/.videopasswd;
        autoindex_exact_size off;
        autoindex on;
        autoindex_format json;
        autoindex_localtime on;
    }

```

以下是一个挂载硬盘的 `/etc/config/fstab` 文件示例

```shell
config 'global'
	option	anon_swap	'0'
	option	anon_mount	'0'
	option	auto_swap	'1'
	option	auto_mount	'1'
	option	delay_root	'5'
	option	check_fs	'0'

config mount
    option target '/mnt/usb'
    option device '/dev/sda1'
    option fstype 'ext4'
    option options 'rw'
    option enabled '1'


config swap
    option device '/mnt/usb/swapfile'
    option enabled '1'
```

**3\. 录制历史文件脚本**

注意需要安装 `ffmpeg` 和 `find`，可以通过 `opkg install ffmpeg findutils-find` 安装。

`record.sh` 脚本每次运行时会录制 5 分钟的 mp4 文件。添加 `*/5 * * * * /root/bin/record.sh >> /var/log/record.log 2>&1 &` `cron` 命令每 5 分钟执行一次。

```shell
#!/bin/bash
# killall -INT ffmpeg

DATE=$(date +%Y-%m-%d)
TIME=$(date +%H:%M:%S)
DIR='/mnt/usb/records'
URI="rtsp://test:123456@192.168.4.158:554"

if [ ! -d "/mnt/usb/" ];then
    echo "/mnt/usb not found."
    exit -1;
fi

mkdir -p "$DIR/$DATE"

ffmpeg -rtsp_transport tcp -i "$URI" -b 900k -r 30 -vcodec copy -an -t 300 "$DIR/$DATE/.$TIME.mp4"
mv "$DIR/$DATE/.$TIME.mp4" "$DIR/$DATE/$TIME.mp4"
```

`delete.sh` 脚本每次运行会删除 15 天前的文件。添加 `0 0 */5 * * /root/bin/delete.sh >> /var/log/delete.log 2>&1 &` `cron` 命令每 5 天执行一次。

```shell
#!/bin/bash

DIR=/mnt/usb/records

find "$DIR" -maxdepth 1 -type d -mtime +15 -exec rm -r {} \;
```







