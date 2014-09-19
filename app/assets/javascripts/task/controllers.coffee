###*
Task controllers.
###
define ->
  'use strict'

  class TaskCtrl
    @$inject = ["$scope", "initialState", "playRoutes", "MessageSocket", "Task", "Project"]
    constructor: (@$scope, @initialState, @playRoutes, @MessageSocket, @Task, @Project) ->
      @$scope.user = @user
      
      @$scope.Task = @Task(@$scope)
      @$scope.Project = @Project(@$scope)

      for own k, v of @initialState
        @$scope[k] = v

      @$scope.projects = @$scope.projects.map((project) =>
        new @$scope.Project(project)
      )

      socketUrl = @playRoutes.controllers.Tasks.socket(@$scope.date.valueOf(), 3).webSocketUrl()
      @$scope.Messenger = new @MessageSocket(socketUrl)

  TaskCtrl: TaskCtrl
