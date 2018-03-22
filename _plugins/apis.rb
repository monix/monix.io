module Jekyll
  class Api3xTag < Liquid::Tag
    def initialize(tag_name, text, tokens)
      super
      @name = text
    end

    def render(context)
      config = context.registers[:site].config
      "#{config["api3x"]}#{@name.strip.gsub(/[.]/, "/")}.html"
    end
  end

  class ScalaAPITag < Liquid::Tag
    def initialize(tag_name, text, tokens)
      super
      @name = text
    end

    def render(context)
      config = context.registers[:site].config
      "#{config["apiScala"]}#{@name.strip.gsub(/[.]/, "/")}.html"
    end
  end
end

Liquid::Template.register_tag('api3x', Jekyll::Api3xTag)

Liquid::Template.register_tag('scala_api', Jekyll::ScalaAPITag)
