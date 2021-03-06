package se.marcuslonnberg.scaladocker.remote.models

import org.joda.time.DateTime

sealed trait ContainerId {
  def value: String
}

case class ContainerHashId(hash: String) extends ContainerId {
  override def value = hash

  def shortHash = hash.take(12)

  override def toString = hash
}

case class ContainerName(name: String) extends ContainerId {
  override def value = name

  override def toString = name
}

sealed trait Port {
  def port: Int

  def protocol: String

  override def toString = s"$port/$protocol"
}

object Port {

  case class Tcp(port: Int) extends Port {
    def protocol = "tcp"
  }

  case class Udp(port: Int) extends Port {
    def protocol = "udp"
  }

  def apply(port: Int, protocol: String): Option[Port] = {
    protocol match {
      case "tcp" => Some(Tcp(port))
      case "udp" => Some(Udp(port))
      case _ => None
    }
  }

  private val PortFormat = "(\\d+)/(\\w+)".r

  def unapply(raw: String): Option[Port] = {
    raw match {
      case PortFormat(port, "tcp") => Some(Tcp(port.toInt))
      case PortFormat(port, "udp") => Some(Udp(port.toInt))
      case _ => None
    }
  }
}

object PortBinding {
  def apply(hostPort: Int): PortBinding = new PortBinding(hostPort = hostPort)
}

case class PortBinding(hostIp: String = "0.0.0.0", hostPort: Int)

/**
 * Configuration for a container.
 *
 * @param image Image to run.
 * @param entryPoint Entry point for the container.
 * @param command Command to run.
 * @param environmentVariables Environment variables.
 * @param exposedPorts Ports that the container should expose.
 * @param volumes Paths inside the container that should be exposed.
 * @param workingDir Working directory for commands to run in.
 * @param user Username or UID.
 * @param hostname Container hostname.
 * @param domainName Container domain name.
 * @param resourceLimits Resource limits.
 * @param standardStreams Configuration for standard streams.
 * @param labels Labels for the container.
 * @param networkDisabled Disable network for the container.
 */
case class ContainerConfig(
  image: ImageName,
  entryPoint: Option[Seq[String]] = None,
  command: Seq[String] = Seq.empty,
  environmentVariables: Seq[String] = Seq.empty,
  exposedPorts: Seq[Port] = Seq.empty,
  volumes: Seq[String] = Seq.empty,
  workingDir: Option[String] = None,
  user: Option[String] = None,
  hostname: Option[String] = None,
  domainName: Option[String] = None,
  standardStreams: StandardStreamsConfig = StandardStreamsConfig(),
  labels: Map[String, String] = Map.empty,
  networkDisabled: Boolean = false
) {
  def withImage(image: ImageName) = {
    copy(image = image)
  }

  def withEntryPoint(args: String*) = {
    copy(entryPoint = Some(args))
  }

  def withCommand(args: String*) = {
    copy(command = args)
  }

  def withEnvironmentVariables(pairs: (String, String)*) = {
    copy(environmentVariables = pairs.map {
      case (key, value) => s"$key=$value"
    })
  }

  def environmentVariablesMap: Map[String, String] = {
    environmentVariables.map { str =>
      str.split("=", 2) match {
        case Array(key, value) => key -> value
        case _ => str -> ""
      }
    }.toMap
  }

  def withExposedPorts(ports: Port*) = {
    copy(exposedPorts = ports)
  }

  def withVolumes(args: String*) = {
    copy(volumes = args)
  }

  def withWorkingDir(workingDir: String) = {
    copy(workingDir = Option(workingDir).filter(_.nonEmpty))
  }

  def withUser(user: String) = {
    copy(user = Option(user).filter(_.nonEmpty))
  }

  def withHostname(hostname: String) = {
    copy(hostname = Option(hostname).filter(_.nonEmpty))
  }

  def withDomainName(domainName: String) = {
    copy(domainName = Option(domainName).filter(_.nonEmpty))
  }

  def withStandardStreams(standardStreams: StandardStreamsConfig) = {
    copy(standardStreams = standardStreams)
  }

  def withLabels(labels: (String, String)*) = {
    copy(labels = Map(labels: _*))
  }

  def withNetworkDisabled(disabled: Boolean) = {
    copy(networkDisabled = disabled)
  }

}

/**
 * Configuration options for standard streams.
 *
 * @param attachStdIn Attach to standard input.
 * @param attachStdOut Attach to standard output.
 * @param attachStdErr Attach to standard error.
 * @param tty Attach standard streams to a tty.
 * @param openStdin Keep stdin open even if not attached.
 * @param stdinOnce Close stdin when one attached client disconnects.
 */
case class StandardStreamsConfig(
  attachStdIn: Boolean = false,
  attachStdOut: Boolean = false,
  attachStdErr: Boolean = false,
  tty: Boolean = false,
  openStdin: Boolean = false,
  stdinOnce: Boolean = false
)

/**
 * Resource limitations on a container.
 *
 * @param memory Memory limit, in bytes.
 * @param memorySwap Total memory limit (memory + swap), in bytes. Set `-1` to disable swap.
 * @param cpuShares CPU shares (relative weight vs. other containers)
 * @param cpuset CPUs in which to allow execution, examples: `"0-2"`, `"0,1"`.
 */
case class ContainerResourceLimits(
  memory: Long = 0,
  memorySwap: Long = 0,
  memoryReservation: Long = 0,
  cpuShares: Long = 0,
  cpuset: Option[String] = None
)

object ContainerLink {
  def unapply(link: String) = {
    link.split(':') match {
      case Array(container) => Some(ContainerLink(container))
      case Array(container, alias) => Some(ContainerLink(container, Some(alias)))
      case _ => None
    }
  }
}

case class ContainerLink(containerName: String, aliasName: Option[String] = None) {
  def mkString = containerName + aliasName.fold("")(":" + _)
}

/**
 * Host configuration for a container.
 *
 * @param portBindings A map of exposed container ports to bindings on the host.
 * @param publishAllPorts Allocate a random port for each exposed container port.
 * @param links Container links.
 * @param volumeBindings Volume bindings.
 * @param volumesFrom Volumes to inherit from other containers.
 * @param devices Devices to add to the container.
 * @param readOnlyRootFilesystem Mount the container's root filesystem as read only.
 * @param dnsServers DNS servers for the container to use.
 * @param dnsSearchDomains DNS search domains.
 * @param networkMode Networking mode for the container
 * @param privileged Gives the container full access to the host.
 * @param capabilities Change Linux kernel capabilities for the container.
 * @param restartPolicy Behavior to apply when the container exits.
 */
case class HostConfig(
  portBindings: Map[Port, Seq[PortBinding]] = Map.empty,
  publishAllPorts: Boolean = false,
  links: Seq[ContainerLink] = Seq.empty,
  volumeBindings: Seq[VolumeBinding] = Seq.empty,
  volumesFrom: Seq[String] = Seq.empty,
  devices: Seq[DeviceMapping] = Seq.empty,
  readOnlyRootFilesystem: Boolean = false,
  dnsServers: Seq[String] = Seq.empty,
  dnsSearchDomains: Seq[String] = Seq.empty,
  networkMode: Option[String] = None,
  privileged: Boolean = false,
  capabilities: LinuxCapabilities = LinuxCapabilities(),
  resourceLimits: ContainerResourceLimits = ContainerResourceLimits(),
  restartPolicy: RestartPolicy = NeverRestart
) {
  def withPortBindings(ports: (Port, Seq[PortBinding])*) = {
    copy(portBindings = Map(ports: _*))
  }

  def withPublishAllPorts(publishAll: Boolean) = {
    copy(publishAllPorts = publishAll)
  }

  def withLinks(links: ContainerLink*) = {
    copy(links = links)
  }

  def withVolumeBindings(volumeBindings: VolumeBinding*) = {
    copy(volumeBindings = volumeBindings)
  }

  def withVolumesFrom(containers: String*) = {
    copy(volumesFrom = containers)
  }

  def withDevices(devices: DeviceMapping*) = {
    copy(devices = devices)
  }

  def withReadOnlyRootFilesystem(readOnlyRootFilesystem: Boolean) = {
    copy(readOnlyRootFilesystem = readOnlyRootFilesystem)
  }

  def withDnsServers(servers: String*) = {
    copy(dnsServers = servers)
  }

  def withDnsSearchDomains(searchDomains: String*) = {
    copy(dnsSearchDomains = searchDomains)
  }

  def withNetworkMode(mode: String) = {
    copy(networkMode = Option(mode))
  }

  def withPrivileged(privileged: Boolean) = {
    copy(privileged = privileged)
  }

  def withCapabilities(capabilities: LinuxCapabilities) = {
    copy(capabilities = capabilities)
  }

  def withRestartPolicy(restartPolicy: RestartPolicy) = {
    copy(restartPolicy = restartPolicy)
  }

  def withResourceLimits(resourceLimits: ContainerResourceLimits) = {
    copy(resourceLimits = resourceLimits)
  }

}

/**
 * @param add Kernel capabilities to add to the container
 * @param drop Kernel capabilities to drop from the container.
 */
case class LinuxCapabilities(
  add: Seq[String] = Seq.empty,
  drop: Seq[String] = Seq.empty
)

trait RestartPolicy {
  def name: String
}

case object NeverRestart extends RestartPolicy {
  val name = ""
}

case object AlwaysRestart extends RestartPolicy {
  val name = "always"
}

object RestartOnFailure {
  val name = "on-failure"
}

case class RestartOnFailure(maximumRetryCount: Int = 0) extends RestartPolicy {
  val name = RestartOnFailure.name
}

case class DeviceMapping(pathOnHost: String, pathInContainer: String, cgroupPermissions: String)

case class CreateContainerResponse(id: ContainerHashId, warnings: Seq[String])

case class ContainerState(
  running: Boolean,
  paused: Boolean,
  restarting: Boolean,
  pid: Int,
  exitCode: Int,
  startedAt: Option[DateTime] = None,
  finishedAt: Option[DateTime] = None
)

case class NetworkSettings(
  ipAddress: String,
  ipPrefixLength: Int,
  gateway: String,
  bridge: String,
  ports: Map[Port, Seq[PortBinding]]
)

case class ContainerInfo(
  id: ContainerHashId,
  created: DateTime,
  path: String,
  args: Seq[String],
  config: ContainerConfig,
  state: ContainerState,
  image: String,
  networkSettings: NetworkSettings,
  resolvConfPath: String,
  hostnamePath: String,
  hostsPath: String,
  name: String,
  mountLabel: Option[String] = None,
  processLabel: Option[String] = None,
  volumes: Seq[VolumeBinding] = Seq.empty,
  hostConfig: HostConfig,
  node: Option[Node]
                        )

object VolumeBinding {
  def unapply(binding: String): Option[VolumeBinding] = {
    binding.split(":") match {
      case Array(host, container) =>
        Some(VolumeBinding(host, container, rw = true))
      case Array(host, container, "ro") =>
        Some(VolumeBinding(host, container, rw = false))
      case _ =>
        None
    }
  }
}

case class VolumeBinding(
  hostPath: String,
  containerPath: String,
  rw: Boolean = true
)

case class Node(
  labels: Map[String, String],
  memory: Long,
  cpus: Long,
  name:String,
  address:String,
  ip: String,
  id:String
)

case class ContainerStatus(
  command: String,
  created: DateTime,
  id: ContainerHashId,
  image: ImageName,
  names: Seq[String],
  ports: Map[Port, Seq[PortBinding]] = Map.empty,
  labels: Map[String, String] = Map.empty,
  status: String
)

