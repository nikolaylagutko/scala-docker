package se.marcuslonnberg.scaladocker.remote.api

case class RegistryAuth(url: String, username: String, password: String) {
  private[api] def toConfig = RegistryAuthConfig(username, password)
}

private[api] case class RegistryAuthConfig(username: String, password: String)