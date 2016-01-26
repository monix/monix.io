require 'ostruct'
require 'yaml'
require "config/lib/helpers"

###
# Site-wide settings
###

@website = OpenStruct.new(YAML::load_file(File.dirname(__FILE__) + "/config/config.yaml")[:website])

###
# Blog settings
###

Time.zone = "Europe/Bucharest"

activate :blog do |blog|
  # blog.prefix = "blog"
  blog.permalink = "blog/:year/:month/:day/:title.html"
  blog.sources = "blog/:year-:month-:day-:title.html"
  blog.taglink = "blog/tags/:tag.html"
  blog.layout = "post"
  # blog.summary_separator = /(READMORE)/
  # blog.summary_length = 250
  blog.year_link = "blog/:year.html"
  blog.month_link = "blog/:year/:month.html"
  blog.day_link = "blog/:year/:month/:day.html"
  # blog.default_extension = ".markdown"

  blog.tag_template = "tag.html"
  blog.calendar_template = "calendar.html"

  # blog.paginate = true
  # blog.per_page = 10
  # blog.page_link = "page/:num"
end

# With no layout
page '/*.xml', layout: false
page '/*.json', layout: false
page '/*.txt', layout: false

# With alternative layout
# page "/path/to/file.html", layout: :otherlayout

# Proxy pages (http://middlemanapp.com/basics/dynamic-pages/)
# proxy "/this-page-has-no-template.html", "/template-file.html", locals: {
#  which_fake_page: "Rendering a fake page with a local variable" }

# Automatic image dimensions on image_tag helper
activate :automatic_image_sizes

set :css_dir, 'assets/css'
set :js_dir, 'assets/js'
set :images_dir, 'assets/img'

activate :syntax

set :markdown_engine, :redcarpet
set :markdown, :fenced_code_blocks => true, :smartypants => true

# Build-specific configuration
configure :build do
  activate :minify_css
  activate :minify_javascript

  # activate :minify_html
  # activate :asset_hash

  #activate :asset_host
  #set :asset_host do |asset|
  #  "//d2uy8r9dr9sdps.cloudfront.net".to_s
  #end

  # Enable cache buster
  # activate :cache_buster

  # Use relative URLs
  # activate :relative_assets
  activate :gzip

  # Compress PNGs after build
  # First: gem install middleman-smusher
  require "middleman-smusher"
  activate :smusher

  # Or use a different image path
  # set :http_path, "/Content/images/"
end
