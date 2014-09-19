###*
Task routes.
###
define (require) ->
  'use strict'

  angular = require("angular")
  moment = require("moment")
  controllers = require("./controllers")
  require("common")

  angular.module("task.routes", ["task.services", "yourprefix.common"])

  .config(["$routeProvider", "initialize", ($routeProvider, initialize) ->
    $routeProvider
      .when("/tasks/:date", {
        templateUrl: "/assets/javascripts/task/tasks.html",
        controller: controllers.TaskCtrl,
        resolve: initialize
      })
      .otherwise(redirectTo: "/tasks/" + moment().startOf("day").format("YYYY-MM-DD"))
  ])
