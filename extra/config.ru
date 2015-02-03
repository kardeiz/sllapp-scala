root = File.expand_path('../..', __FILE__)

lib  = File.join(root, 'src/main/ruby')
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

Dir[File.join(root, 'target/curbi/WEB-INF/lib/*.jar')].each do |jar|
  require jar
end

$MODE = 'development'

ENV['GEM_PATH'] = File.join(root, 'target/rubygems')
require 'rubygems'
Gem.clear_paths

require 'config/initializer'

run App


