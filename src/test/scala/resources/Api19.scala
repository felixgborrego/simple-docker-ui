package resources

/**
 * Changes introduces in V19
 */
object ApiV19 {


  val Containers = """[{"Id":"d6b37b8ff5dea53f6407a5b8bbdcae7e94fc0a970eddb970f423bb2bf2db7c65","Names":["/hopeful_ramanujan"],"Image":"mongo:3.0","ImageID":"db8536bb5482363680a96144545c8f0cba488ac8184e162ec825e00f9b7e3a9c","Command":"/entrypoint.sh mongod","Created":1451941946,"Ports":[{"IP":"0.0.0.0","PrivatePort":27017,"PublicPort":32768,"Type":"tcp"}],"Labels":{},"Status":"Up About an hour","HostConfig":{"NetworkMode":"default"}}]"""


  val ContainerStats = """{
                         |
                         |   "read": "2015-12-08T21:00:33.35631128Z",
                         |
                         |   "network": {
                         |
                         |      "rx_bytes": 648,
                         |
                         |      "rx_packets": 8,
                         |
                         |      "rx_errors": 0,
                         |
                         |      "rx_dropped": 0,
                         |
                         |      "tx_bytes": 738,
                         |
                         |      "tx_packets": 9,
                         |
                         |      "tx_errors": 0,
                         |
                         |      "tx_dropped": 0
                         |
                         |   },
                         |
                         |   "precpu_stats": {
                         |
                         |      "cpu_usage": {
                         |
                         |         "total_usage": 10570644357,
                         |
                         |         "percpu_usage": [
                         |
                         |            1340203476,
                         |
                         |            1979888251,
                         |
                         |            660652332,
                         |
                         |            1278684774,
                         |
                         |            1721398875,
                         |
                         |            884397054,
                         |
                         |            1508832814,
                         |
                         |            1196586781
                         |
                         |         ],
                         |
                         |         "usage_in_kernelmode": 3310000000,
                         |
                         |         "usage_in_usermode": 3760000000
                         |
                         |      },
                         |
                         |      "system_cpu_usage": 284963260000000,
                         |
                         |      "throttling_data": {
                         |
                         |         "periods": 0,
                         |
                         |         "throttled_periods": 0,
                         |
                         |         "throttled_time": 0
                         |
                         |      }
                         |
                         |   },
                         |
                         |   "cpu_stats": {
                         |
                         |      "cpu_usage": {
                         |
                         |         "total_usage": 10570644357,
                         |
                         |         "percpu_usage": [
                         |
                         |            1340203476,
                         |
                         |            1979888251,
                         |
                         |            660652332,
                         |
                         |            1278684774,
                         |
                         |            1721398875,
                         |
                         |            884397054,
                         |
                         |            1508832814,
                         |
                         |            1196586781
                         |
                         |         ],
                         |
                         |         "usage_in_kernelmode": 3310000000,
                         |
                         |         "usage_in_usermode": 3760000000
                         |
                         |      },
                         |
                         |      "system_cpu_usage": 284971240000000,
                         |
                         |      "throttling_data": {
                         |
                         |         "periods": 0,
                         |
                         |         "throttled_periods": 0,
                         |
                         |         "throttled_time": 0
                         |
                         |      }
                         |
                         |   },
                         |
                         |   "memory_stats": {
                         |
                         |      "usage": 53194752,
                         |
                         |      "max_usage": 55910400,
                         |
                         |      "stats": {
                         |
                         |         "active_anon": 4104192,
                         |
                         |         "active_file": 14872576,
                         |
                         |         "cache": 50151424,
                         |
                         |         "hierarchical_memory_limit": 9223372036854771712,
                         |
                         |         "hierarchical_memsw_limit": 9223372036854771712,
                         |
                         |         "inactive_anon": 10182656,
                         |
                         |         "inactive_file": 23621632,
                         |
                         |         "mapped_file": 11079680,
                         |
                         |         "pgfault": 384596,
                         |
                         |         "pgmajfault": 0,
                         |
                         |         "pgpgin": 315004,
                         |
                         |         "pgpgout": 302118,
                         |
                         |         "rss": 2629632,
                         |
                         |         "rss_huge": 0,
                         |
                         |         "swap": 0,
                         |
                         |         "total_active_anon": 4104192,
                         |
                         |         "total_active_file": 14872576,
                         |
                         |         "total_cache": 50151424,
                         |
                         |         "total_inactive_anon": 10182656,
                         |
                         |         "total_inactive_file": 23621632,
                         |
                         |         "total_mapped_file": 11079680,
                         |
                         |         "total_pgfault": 384596,
                         |
                         |         "total_pgmajfault": 0,
                         |
                         |         "total_pgpgin": 315004,
                         |
                         |         "total_pgpgout": 302118,
                         |
                         |         "total_rss": 2629632,
                         |
                         |         "total_rss_huge": 0,
                         |
                         |         "total_swap": 0,
                         |
                         |         "total_unevictable": 0,
                         |
                         |         "total_writeback": 0,
                         |
                         |         "unevictable": 0,
                         |
                         |         "writeback": 0
                         |
                         |      },
                         |
                         |      "failcnt": 0,
                         |
                         |      "limit": 2099122176
                         |
                         |   },
                         |
                         |   "blkio_stats": {
                         |
                         |      "io_service_bytes_recursive": [],
                         |
                         |      "io_serviced_recursive": [],
                         |
                         |      "io_queue_recursive": [],
                         |
                         |      "io_service_time_recursive": [],
                         |
                         |      "io_wait_time_recursive": [],
                         |
                         |      "io_merged_recursive": [],
                         |
                         |      "io_time_recursive": [],
                         |
                         |      "sectors_recursive": []
                         |
                         |   }
                         |
                         |}""".stripMargin
}
