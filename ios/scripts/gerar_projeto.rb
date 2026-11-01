# Gera o ExemploSDK.xcodeproj a partir dos arquivos em ExemploSDK/.
# Depois rode "pod install" e abra o ExemploSDK.xcworkspace (veja o README).
#
#   ruby scripts/gerar_projeto.rb
#
# Usa a gem "xcodeproj", que vem junto com o CocoaPods.

require 'xcodeproj'

Dir.chdir(File.expand_path('..', __dir__))

projeto = Xcodeproj::Project.new('ExemploSDK.xcodeproj')
alvo = projeto.new_target(:application, 'ExemploSDK', :ios, '16.0', nil, :swift)

grupo = projeto.main_group.new_group('ExemploSDK', 'ExemploSDK')
Dir.glob('ExemploSDK/*.swift').sort.each do |arquivo|
  alvo.add_file_references([grupo.new_reference(File.basename(arquivo))])
end
alvo.add_resources([grupo.new_reference('Assets.xcassets')])
grupo.new_reference('Info.plist')

configs = projeto.main_group.new_group('Configuracao', 'Configuracao')
xcconfig = {
  'Debug' => configs.new_reference('Debug.xcconfig'),
  'Release' => configs.new_reference('Release.xcconfig')
}
configs.new_reference('Comum.xcconfig')
configs.new_reference('Local.xcconfig.exemplo')

alvo.build_configurations.each do |config|
  # Debug.xcconfig / Release.xcconfig incluem o xcconfig do CocoaPods e o Local.xcconfig.
  config.base_configuration_reference = xcconfig[config.name]
  s = config.build_settings
  s['INFOPLIST_FILE'] = 'ExemploSDK/Info.plist'
  s['IPHONEOS_DEPLOYMENT_TARGET'] = '16.0'
  s['TARGETED_DEVICE_FAMILY'] = '1,2'
  s['SWIFT_VERSION'] = '5.0'
  s['CODE_SIGN_STYLE'] = 'Automatic'
  s['ENABLE_USER_SCRIPT_SANDBOXING'] = 'NO'
  s['ASSETCATALOG_COMPILER_APPICON_NAME'] = ''
  # Estes vêm do Comum.xcconfig / Local.xcconfig: não podem ser definidos aqui.
  s.delete('PRODUCT_BUNDLE_IDENTIFIER')
  s.delete('DEVELOPMENT_TEAM')
end

projeto.save

esquema = Xcodeproj::XCScheme.new
esquema.configure_with_targets(alvo, nil, launch_target: true)
esquema.save_as(projeto.path, 'ExemploSDK', true)

puts 'ExemploSDK.xcodeproj gerado'
