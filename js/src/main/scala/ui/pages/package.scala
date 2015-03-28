package ui

import japgolly.scalajs.react.ReactElement

package object pages {

  trait Page{
    def id:String
    def component:ReactElement
  }

}
