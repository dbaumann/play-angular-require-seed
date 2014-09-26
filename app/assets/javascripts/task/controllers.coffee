###*
Task controllers.
###
define (require) ->
  'use strict'

  moment = require("moment")

  class TaskCtrl
    @$inject = ["$scope", "$window", "$location", "initialState", "playRoutes", "MessageSocket", "IdentityMap", "Task", "Project"]
    constructor: (@$scope, @$window, @$location, @initialState, @playRoutes, @MessageSocket, @IdentityMap, @Task, @Project) ->
      @$scope.Project = @Project(@$scope)
      @$scope.Task = @Task(@$scope)

      for own k, v of @initialState
        @$scope[k] = v

      if(@$scope.projects?)
        @$scope.projects = @$scope.projects.map((project) =>
          new @$scope.Project(project)
        )
      else
        @$scope.projects = []

      socketUrl = @playRoutes.controllers.Tasks.socket(@$scope.date.valueOf(), 3).webSocketUrl()
      @$scope.Messenger = new @MessageSocket(socketUrl)
      @$scope.Messenger.ask = @$scope.Messenger.withTimeout(5000)

      @$scope.Project.openChannel()
      @$scope.Task.openChannel()

      @$scope.nextDay = =>
        @$scope.Messenger.close()
        @IdentityMap.clear()
        date = @$scope.date.toDate()
        date.setDate(date.getDate() + 1)
        @$location.path("/tasks/" + moment(date).format("YYYY-MM-DD"))

      @$scope.previousDay = =>
        @$scope.Messenger.close()
        @IdentityMap.clear()
        date = @$scope.date.toDate()
        date.setDate(date.getDate() - 1)
        @$location.path("/tasks/" + moment(date).format("YYYY-MM-DD"))

  TaskCtrl: TaskCtrl
