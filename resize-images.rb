#!/usr/bin/env ruby

# Resizes android resources for lower dpi screens

require 'RMagick'

sizes = [
  {
    name: "xxhdpi",
    dpi: 480
  },
  {
    name: "xhdpi",
    dpi: 320
  },
  {
    name: "hdpi",
    dpi: 240
  },
  {
    name: "mdpi",
    dpi: 160
  }
]

def size_path(size)
  "Guita/src/main/res/drawable-#{size[:name]}"
end

sizes.each do |size|
  Dir.foreach(size_path(size)) do |img|
    next if img.start_with? "."
    sizes.each do |small|
      next if small[:dpi] >= size[:dpi]
      next if File.exists? "#{size_path(small)}/#{img}"
      next if img.end_with? ".9.png"
      puts "Resize #{img} from #{size[:name]} to #{small[:name]}"

      image = Magick::Image.read("#{size_path(size)}/#{img}").first
      newimg = image.resize([image.columns * small[:dpi] / size[:dpi], 1].max,
                            [image.rows * small[:dpi] / size[:dpi], 1].max)
      newimg.write("#{size_path(small)}/#{img}")
    end
  end  
end
