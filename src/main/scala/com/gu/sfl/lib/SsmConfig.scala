package com.gu.sfl.lib

import com.gu.{AppIdentity, AwsIdentity}
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.sfl.Logging

class SsmConfig(defaultAppName: String) extends Logging {
  val identity: AppIdentity = logOnThrown(() => AppIdentity.whoAmI(defaultAppName = defaultAppName), "Error retrieving appIdentity")
  val config = logOnThrown(() => ConfigurationLoader.load(identity) {
    case AwsIdentity(app, stack, stage, _) => SSMConfigurationLocation(path = s"/$app/$stage/$stack")
  }, "Errr reading config from ssm")
}
