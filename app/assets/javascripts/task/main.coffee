###*
task/main.js is the entry module which serves as an entry point so other modules only have
to include a single module.
###
define (require) ->
  'use strict'

  angular = require("angular")
  require("angular-animate")
  require("angular-xeditable")
  require("angular-message-socket")
  require("angular-identity-map")

  controllers = require("./controllers")
  require("./routes")
  require("./services")
  require("./models")

  angular.module("yourprefix.tasks", [
    "ngRoute",
    "ngAnimate",
    "xeditable",
    "dbaumann.message-socket",
    "dbaumann.identity-map",
    "task.routes", "task.services", "task.models"
  ])

  .controller("TaskCtrl", controllers.TaskCtrl)

  .run((editableOptions, editableThemes) ->
    editableOptions.theme = "bs3"
    editableThemes.bs3.inputClass = "input-sm"
    editableThemes.bs3.buttonsClass = "btn-sm"
  )

