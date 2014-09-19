###*
Task services.
###
define (require) ->
  angular = require("angular")
  moment = require("moment")
  require("common")

  angular.module("task.services", ["yourprefix.common"])

  .constant("initialize",
    initialState: ["$route", "$q", "$http", "playRoutes",
      ($route, $q, $http, playRoutes) ->
        deferred = $q.defer()
        resolve = {}

        parsedDate = moment($route.current.params.date)
        if(parsedDate.isValid())
          resolve.title = "Tasks for " + parsedDate.format("dddd MMMM Do, YYYY")
          resolve.date = parsedDate
          stamp = resolve.date.valueOf()

          playRoutes.controllers.Tasks.initialize(stamp).get().then(({data}) ->
            resolve.projects = data.projects
            resolve.defaults = data.defaults
            deferred.resolve(resolve)
          )

        deferred.promise
    ]
  )
