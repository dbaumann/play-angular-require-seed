###*
Task models.
###
define (require) ->
  'use strict'

  angular = require("angular")
  moment = require("moment")

  uniqueId = -> Math.random().toString(36).substr(2, 9)

  angular.module("task.models", ["yourprefix.common"])

  .factory("Task", ["$timeout", "IdentityMap", ($timeout, IdentityMap) ->
    ($scope) ->
      class Task
        constructor: (attrs) ->
          for k, v of attrs
            this[k] = v
          IdentityMap.set(@constructor)(this)

        identity: -> @id

        updated: ->
          $scope.Messenger.tell(_: "TaskUpdate", task: this)

        @openChannel: ->
          $scope.Messenger.receive("TaskAddDone", (event) ->
            project = IdentityMap.get($scope.Project)(event.projectId)
            project.tasks.push(new $scope.Task(event.task))
          )

          $scope.Messenger.receive("TaskUpdateDone", (event) ->
            IdentityMap.update($scope.Task, branches = ["title", "description", "assignedTo"])(event.task)
          )

          $scope.Messenger.receive("TaskDeleteDone", (event) ->
            project = IdentityMap.get($scope.Project)(event.projectId)
            task = IdentityMap.get($scope.Task)(event.task)

            index = project.tasks.indexOf(task)
            project.tasks.splice(index, 1)
          )
  ])

  .factory("Project", ["IdentityMap", (IdentityMap) ->
    ($scope) ->
      class Project
        constructor: (attrs) ->
          for k, v of attrs
            this[k] = v
          IdentityMap.set(@constructor)(this)
          
          @nextTask = angular.copy($scope.defaults.task)
          @tasks = @tasks.map((task) =>
            new $scope.Task(task)
          )

        identity: -> @id

        @new: ->
          nextProject = angular.copy($scope.defaults.project)
          nextProject.name = "project " + uniqueId()
          $scope.Messenger.tell(_: "ProjectAdd", project: nextProject)

        updated: ->
          $scope.Messenger.tell(_: "ProjectUpdate", project: this)

        addTask: ->
          if(@nextTask.title.length)
            $scope.Messenger.ask(_: "TaskAdd", task: @nextTask, projectId: @id)
              .for("TaskAddDone").then(=>
                @nextTask = angular.copy($scope.defaults.task)
              )

        removeTask: (index) ->
          $scope.Messenger.tell(_: "TaskDelete", task: @tasks[index], projectId: @id)

        @openChannel: ->
          $scope.Messenger.receive("ProjectAddDone", (event) ->
            $scope.projects.push(new $scope.Project(event.project))
          )

          $scope.Messenger.receive("ProjectUpdateDone", (event) ->
            IdentityMap.update($scope.Project, branches = ["name"])(event.project)
          )
  ])



