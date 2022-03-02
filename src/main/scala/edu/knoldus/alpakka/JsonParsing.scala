package edu.knoldus.alpakka

import edu.knoldus.alpakka.GraphMessages.{Account, AccountAddressData, AccountPersonalData}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


object JsonParsing extends DefaultJsonProtocol {

  implicit val accountFormat: RootJsonFormat[Account] = jsonFormat13(Account)
  implicit val accountPersonalFormat: RootJsonFormat[AccountPersonalData] = jsonFormat7(AccountPersonalData)
  implicit val accountAddressFormat: RootJsonFormat[AccountAddressData] = jsonFormat7(AccountAddressData)

}