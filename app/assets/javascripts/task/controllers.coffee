###*
Task controllers.
###
define (require) ->
  'use strict'

  traverse = require("traverse")

  class TaskCtrl
    @$inject = ["$scope", "$window", "$location", "initialState", "playRoutes", "MessageSocket", "Task", "Project"]
    constructor: (@$scope, @$window, @$location, @initialState, @playRoutes, @MessageSocket, @Task, @Project) ->
      @$window.traverse = traverse

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
        date = @$scope.date.toDate()
        date.setDate(date.getDate() + 1)
        @$location.path("/tasks/" + moment(date).format("YYYY-MM-DD"))

      @$scope.previousDay = =>
        date = @$scope.date.toDate()
        date.setDate(date.getDate() - 1)
        @$location.path("/tasks/" + moment(date).format("YYYY-MM-DD"))

  TaskCtrl: TaskCtrl
