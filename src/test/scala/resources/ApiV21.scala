package resources

object ApiV21 {
  object SwarmMaster {
    val Info =
      """
        |{
        |  "ID": "",
        |  "Containers": 14,
        |  "Driver": "",
        |  "DriverStatus": [
        |    [
        |      "\bRole",
        |      "primary"
        |    ],
        |    [
        |      "\bStrategy",
        |      "spread"
        |    ],
        |    [
        |      "\bFilters",
        |      "health, port, dependency, affinity, constraint"
        |    ],
        |    [
        |      "\bNodes",
        |      "2"
        |    ],
        |    [
        |      "swarm-agent-00",
        |      "192.168.99.102:2376"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Containers",
        |      "7"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Reserved CPUs",
        |      "0 \/ 1"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Reserved Memory",
        |      "0 B \/ 1.021 GiB"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Labels",
        |      "executiondriver=native-0.2, kernelversion=4.1.12-boot2docker, operatingsystem=Boot2Docker 1.9.0 (TCL 6.4); master : 16e4a2a - Tue Nov  3 19:49:22 UTC 2015, provider=virtualbox, storagedriver=aufs"
        |    ],
        |    [
        |      "swarm-node-01",
        |      "192.168.99.103:2376"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Containers",
        |      "7"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Reserved CPUs",
        |      "0 \/ 1"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Reserved Memory",
        |      "0 B \/ 1.021 GiB"
        |    ],
        |    [
        |      " \u00e2\u201d\u201d Labels",
        |      "executiondriver=native-0.2, kernelversion=4.1.12-boot2docker, operatingsystem=Boot2Docker 1.9.0 (TCL 6.4); master : 16e4a2a - Tue Nov  3 19:49:22 UTC 2015, provider=virtualbox, storagedriver=aufs"
        |    ]
        |  ],
        |  "ExecutionDriver": "",
        |  "Images": 5,
        |  "KernelVersion": "",
        |  "OperatingSystem": "",
        |  "NCPU": 2,
        |  "MemTotal": 2193614438,
        |  "Name": "a90ad8e7feb6",
        |  "Labels": null,
        |  "Debug": false,
        |  "NFd": 0,
        |  "NGoroutines": 0,
        |  "SystemTime": "2015-11-21T10:54:41.414848931Z",
        |  "NEventsListener": 1,
        |  "InitPath": "",
        |  "InitSha1": "",
        |  "IndexServerAddress": "",
        |  "MemoryLimit": true,
        |  "SwapLimit": true,
        |  "IPv4Forwarding": true,
        |  "BridgeNfIptables": true,
        |  "BridgeNfIp6tables": true,
        |  "DockerRootDir": "",
        |  "HttpProxy": "",
        |  "HttpsProxy": "",
        |  "NoProxy": ""
        |}
      """.stripMargin

  }

  val Containers =
    """[{"Id":"e99b1c271bfe564c5ce9dc279d8ab5388adea801f263bc1352ca6c7ff2c32a40","Names":["/clever_cray"],"Image":"nginx:latest","ImageID":"198a73cfd6864ec3d349cf8f146382cca9584a56c3b80f28b7318c9895fb0ae3","Command":"nginx -g 'daemon off;'","Created":1448213291,"Ports":[{"IP":"0.0.0.0","PrivatePort":443,"PublicPort":32768,"Type":"tcp"},{"IP":"0.0.0.0","PrivatePort":80,"PublicPort":32769,"Type":"tcp"}],"Labels":{"com.docker.swarm.id":"c94a11d199eceb23ebe3bb01e921453dc5115b370e549bc2bb9c0b3dd721977c"},"Status":"Up 4 hours","HostConfig":{"NetworkMode":"default"}},{"Id":"a2bb5254238873751d06ab96cf8dd4606e02c457bbd8cf76142054775a18486b","Names":["/swarm-agent"],"Image":"swarm:latest","ImageID":"6b40fe7724bd29107f6182ca2befec011cdf524b23ebc4c9a33591d6b7aea4ee","Command":"/swarm join --advertise 192.168.99.102:2376 token://459965dc11aa6394ed03e9600d6496e0","Created":1448213026,"Ports":[{"PrivatePort":2375,"Type":"tcp"}],"Labels":{},"Status":"Up 4 hours","HostConfig":{"NetworkMode":"default"}}] """

  val ContainerInfo = """{"Id":"ffef6e062e114e4bed146755f392f05f663a22bfca6b59746689f65cccbdbf33","Created":"2015-11-22T14:25:49.338852911Z","Path":"nginx","Args":["-g","daemon off;"],"State":{"Status":"running","Running":true,"Paused":false,"Restarting":false,"OOMKilled":false,"Dead":false,"Pid":9217,"ExitCode":0,"Error":"","StartedAt":"2015-11-22T14:25:49.434251975Z","FinishedAt":"0001-01-01T00:00:00Z"},"Image":"8d5e6665a7a6e3e38929d737206f6e4bf20574bfe696d1bc30bf572034bf81de","ResolvConfPath":"/mnt/sda1/var/lib/docker/containers/ffef6e062e114e4bed146755f392f05f663a22bfca6b59746689f65cccbdbf33/resolv.conf","HostnamePath":"/mnt/sda1/var/lib/docker/containers/ffef6e062e114e4bed146755f392f05f663a22bfca6b59746689f65cccbdbf33/hostname","HostsPath":"/mnt/sda1/var/lib/docker/containers/ffef6e062e114e4bed146755f392f05f663a22bfca6b59746689f65cccbdbf33/hosts","LogPath":"/mnt/sda1/var/lib/docker/containers/ffef6e062e114e4bed146755f392f05f663a22bfca6b59746689f65cccbdbf33/ffef6e062e114e4bed146755f392f05f663a22bfca6b59746689f65cccbdbf33-json.log","Node":{"ID":"AU56:6SXK:VMZX:354Y:JSA7:FKRT:3K6G:UMQE:P6QX:JLDN:AQ5E:RRCZ","IP":"192.168.99.102","Addr":"192.168.99.102:2376","Name":"swarm-agent-00","Cpus":1,"Memory":1044578304,"Labels":{"executiondriver":"native-0.2","kernelversion":"4.1.12-boot2docker","operatingsystem":"Boot2Docker 1.9.0 (TCL 6.4); master : 16e4a2a - Tue Nov  3 19:49:22 UTC 2015","provider":"virtualbox","storagedriver":"aufs"}},"Name":"/cocky_easley","RestartCount":0,"Driver":"aufs","ExecDriver":"native-0.2","MountLabel":"","ProcessLabel":"","AppArmorProfile":"","ExecIDs":null,"HostConfig":{"Binds":null,"ContainerIDFile":"","LxcConf":[],"Memory":0,"MemoryReservation":0,"MemorySwap":0,"KernelMemory":0,"CpuShares":0,"CpuPeriod":0,"CpusetCpus":"","CpusetMems":"","CpuQuota":0,"BlkioWeight":0,"OomKillDisable":false,"MemorySwappiness":-1,"Privileged":false,"PortBindings":{},"Links":null,"PublishAllPorts":false,"Dns":null,"DnsOptions":null,"DnsSearch":null,"ExtraHosts":null,"VolumesFrom":null,"Devices":[],"NetworkMode":"default","IpcMode":"","PidMode":"","UTSMode":"","CapAdd":null,"CapDrop":null,"GroupAdd":null,"RestartPolicy":{"Name":"no","MaximumRetryCount":0},"SecurityOpt":null,"ReadonlyRootfs":false,"Ulimits":null,"LogConfig":{"Type":"json-file","Config":{}},"CgroupParent":"","ConsoleSize":[0,0],"VolumeDriver":""},"GraphDriver":{"Name":"aufs","Data":null},"Mounts":[{"Name":"f5ef95bbfcaac00f4325fb086a48dc3ca4a362ccc82afd7ad4ddd3af67dc0853","Source":"/mnt/sda1/var/lib/docker/volumes/f5ef95bbfcaac00f4325fb086a48dc3ca4a362ccc82afd7ad4ddd3af67dc0853/_data","Destination":"/var/cache/nginx","Driver":"local","Mode":"","RW":true}],"Config":{"Hostname":"ffef6e062e11","Domainname":"","User":"","AttachStdin":false,"AttachStdout":false,"AttachStderr":false,"ExposedPorts":{"443/tcp":{},"80/tcp":{}},"Tty":false,"OpenStdin":false,"StdinOnce":false,"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","NGINX_VERSION=1.9.7-1~jessie"],"Cmd":["nginx","-g","daemon off;"],"Image":"nginx","Volumes":{"/var/cache/nginx":{}},"WorkingDir":"","Entrypoint":null,"OnBuild":null,"Labels":{"com.docker.swarm.id":"bca0329a7f0cf3ac1f2a245d140ad10806e9c862d247fcfa31abafb2057a4a28"},"StopSignal":"SIGTERM"},"NetworkSettings":{"Bridge":"","SandboxID":"1bfa94c72c89b79b9b3236654770778b4a8ce49877594c7aff1a98f542b630d1","HairpinMode":false,"LinkLocalIPv6Address":"","LinkLocalIPv6PrefixLen":0,"Ports":{"443/tcp":null,"80/tcp":null},"SandboxKey":"/var/run/docker/netns/1bfa94c72c89","SecondaryIPAddresses":null,"SecondaryIPv6Addresses":null,"EndpointID":"2d6b35d29065588a057ebe8bbc72c98d094fcbfc372060c26eae825d6458af5d","Gateway":"172.17.0.1","GlobalIPv6Address":"","GlobalIPv6PrefixLen":0,"IPAddress":"172.17.0.3","IPPrefixLen":16,"IPv6Gateway":"","MacAddress":"02:42:ac:11:00:03","Networks":{"bridge":{"EndpointID":"2d6b35d29065588a057ebe8bbc72c98d094fcbfc372060c26eae825d6458af5d","Gateway":"172.17.0.1","IPAddress":"172.17.0.3","IPPrefixLen":16,"IPv6Gateway":"","GlobalIPv6Address":"","GlobalIPv6PrefixLen":0,"MacAddress":"02:42:ac:11:00:03"}}}}"""

  val ImageInfoWithoutRepoTags =
    """
      |  {
      |    "Id": "dcfdc1bcfbf49d665ee545abb904662b4525d7ebe0d9550e609a2013560ea8a6",
      |    "ParentId": "",
      |    "RepoTags": null,
      |    "RepoDigests": [
      |      "foo/bar@488403bc9e67ff152b3145b4611dec260b8f7d2bd82e73a3a660ea770930f098"
      |    ],
      |    "Created": 1476176739,
      |    "Size": 747488568,
      |    "VirtualSize": 747488568,
      |    "Labels": {}
      |  }
    """.stripMargin

}
