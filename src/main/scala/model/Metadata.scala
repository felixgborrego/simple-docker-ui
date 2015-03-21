package model

case class Connection(url: String)

case class DockerMetadata(connection: Connection, info: Info, version: Version, containers: Seq[Container])

case class Info(Containers: Int,
                Debug: Int,
                Driver: String,
                ExecutionDriver: String,
                IPv4Forwarding: Int,
                Images: Int,
                IndexServerAddress: String,
                InitPath: String,
                KernelVersion: String,
                MemTotal: Float,
                MemoryLimit: Int,
                NEventsListener: Int,
                NGoroutines: Int,
                OperatingSystem: String,
                SwapLimit: Int)

case class Version(ApiVersion: String,
                   Arch: String,
                   GitCommit: String,
                   GoVersion: String,
                   KernelVersion: String,
                   Os: String,
                   Version: String)

