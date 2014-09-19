###*
Task models.
###
define (require) ->
  'use strict'

  angular = require("angular")
  moment = require("moment")

  uniqueId = -> Math.random().toString(36).substr(2, 9)

  angular.module("task.models", ["yourprefix.common"])

  .factory("Task", -> ($scope) ->
    class Task
      constructor: (attrs) ->
        for k, v of attrs
          this[k] = v
  )

  .factory("Project", -> ($scope) ->
    class Project
      constructor: (attrs) ->
        for k, v of attrs
          this[k] = v
        
        @nextTask = angular.copy($scope.defaults.task)
        @tasks = @tasks.map((task) =>
          new $scope.Task(task)
        )

      @new: ->
        nextProject = angular.copy($scope.defaults.project)
        nextProject.name = "project " + uniqueId()
        $scope.projects.push(new $scope.Project(nextProject))

      addTask: ->
        @tasks.push(@nextTask)
        @nextTask = angular.copy($scope.defaults.task)

      removeTask: (index) ->
        @tasks.splice(index, 1)
  )



