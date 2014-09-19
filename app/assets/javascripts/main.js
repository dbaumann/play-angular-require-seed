// `main.js` is the file that sbt-web will use as an entry point
(function (requirejs) {
  'use strict';

  // -- RequireJS config --
  requirejs.config({
    // Packages = top-level folders; loads a contained file named 'main.js"
    packages: ['common', 'home', 'user', 'dashboard', 'task'],
    shim: {
      'jsRoutes': {
        deps: [],
        // it's not a RequireJS module, so we have to tell it what var is returned
        exports: 'jsRoutes'
      },
      // Hopefully this all will not be necessary but can be fetched from WebJars in the future
      'angular': {
        deps: ['jquery'],
        exports: 'angular'
      },
      'angular-route': ['angular'],
      'angular-cookies': ['angular'],
      'angular-animate': ['angular'],
      'angular-message-socket': ['angular'],
      'angular-identity-map': ['angular'],
      'angular-xeditable': ['angular'],
      'bootstrap': ['jquery']
    },
    paths: {
      'requirejs': ['../lib/requirejs/require'],
      'jquery': ['../lib/jquery/jquery'],
      'angular': ['../lib/angularjs/angular'],
      'angular-route': ['../lib/angularjs/angular-route'],
      'angular-cookies': ['../lib/angularjs/angular-cookies'],
      'angular-animate': ['../lib/angular-animate'],
      'angular-message-socket': ['../lib/angular-message-socket'],
      'angular-identity-map': ['../lib/angular-identity-map'],
      'angular-xeditable': ['../lib/angular-xeditable/js/xeditable'],
      'bootstrap': ['../lib/bootstrap/js/bootstrap'],
      'moment': ['../lib/momentjs/moment'],
      'jsRoutes': ['/jsroutes']
    }
  });

  requirejs.onError = function (err) {
    console.log(err);
  };

  // Load the app. This is kept minimal so it doesn't need much updating.
  require(['angular', 'angular-cookies', 'angular-route', 'jquery', 'bootstrap', './app'],
    function (angular) {
      angular.bootstrap(document, ['app']);
    }
  );
})(requirejs);
