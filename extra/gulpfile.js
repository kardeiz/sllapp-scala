var gulp = require('gulp');
var concat = require('gulp-concat');
var less = require('gulp-less');
var rename = require("gulp-rename");

gulp.task('less', function () {
  gulp.src('./custom.less')
    .pipe(less({
      paths: [ 'bower_components' ]
    }))
    .pipe(rename("application.css"))
    .pipe(gulp.dest('../src/main/webapp/assets/css/'));
});



gulp.task('scripts', function() {
  gulp.src([
    'bower_components/jquery/dist/jquery.min.js',
    'bower_components/bootstrap-datepicker/js/bootstrap-datepicker.js',
    'custom.js'
  ])
  .pipe(concat('application.js'))
  .pipe(gulp.dest('../src/main/webapp/assets/js/'))
});

gulp.task('default', [ 'less', 'scripts' ]);
