# Diamond RP Launcher

Launcher nativo em Kotlin para o servidor SA-MP Mobile **Diamond RP**.

## Projeto completo — todas as 3 etapas entregues

- **Splash Screen**: baixa o `config.json` remoto, decide entre tela principal,
  atualização obrigatória ou manutenção.
- **Tela Principal**: nickname, botão JOGAR, status do servidor ao vivo (query UDP
  nativa do SA-MP: players/ping), notícias em cards, redes sociais, abre o cliente
  SA-MP Mobile instalado.
- **Tela de Atualização**: changelog, download em segundo plano via `DownloadService`
  (foreground service) com **pausar/continuar/cancelar**, progresso, velocidade (MB/s),
  tempo restante, verificação de integridade (SHA-256) e extração automática de `.zip`.
- **Tela de Configurações**: tema (sistema/escuro/claro), idioma (PT-BR/EN — troca
  em tempo real), limpar cache (com tamanho exibido), verificar atualização, sobre.
- **Tela Sobre**: versão do app, descrição, redes sociais (carregadas do config remoto).
- **GitHub Actions**: workflow pronto (`.github/workflows/build.yml`) que gera o
  Gradle Wrapper automaticamente e compila **APK Debug e Release**, disponíveis
  como Artifacts — não precisa do Android Studio.

## Como gerar o APK pelo GitHub (sem precisar de computador/Android Studio)

1. Crie um repositório novo no GitHub (pode ser pelo próprio app do GitHub no celular).
2. Envie todo o conteúdo desta pasta `DiamondRPLauncher/` para a raiz do repositório
   (pelo app do GitHub: "Add file" > "Upload files", ou `git push` se tiver acesso a um PC).
3. Vá na aba **Actions** do repositório — o workflow "Build Diamond RP Launcher"
   roda automaticamente a cada push na branch `main` (ou dispare manualmente em
   "Run workflow").
4. Quando o build terminar (ícone verde ✔️), abra a execução e baixe os **Artifacts**:
   `diamondrp-launcher-debug` (APK debug, pronto para instalar/testar) e
   `diamondrp-launcher-release` (APK release, **sem assinatura** — precisa ser
   assinado antes de publicar em lojas, mas instala normalmente para testes se
   habilitar "fontes desconhecidas").
5. Baixe o `.zip` do artifact no celular e instale o `.apk` que está dentro.

## Antes de gerar o build final (ajustes obrigatórios)

Edite estes dois arquivos antes de compilar para produção:

- `app/src/main/java/com/diamondrp/launcher/utils/Constants.kt`
  - `REMOTE_CONFIG_URL`: aponte para o seu `config.json` real hospedado
    (veja `config.example.json` como modelo).
  - `SAMP_CLIENT_PACKAGE`: package do cliente SA-MP Mobile usado pelos seus
    jogadores (necessário para o botão JOGAR abrir o jogo).
- `app/src/main/AndroidManifest.xml`
  - Dentro de `<queries>`, troque `com.rockstargames.gtasa` pelo mesmo package
    definido acima (precisa ser idêntico nos dois lugares).

## Assets pendentes (arte)

Estes drawables são **placeholders funcionais** (formas vetoriais simples) —
troque pela arte oficial da marca quando tiver:

- `res/drawable/ic_diamond_logo.xml` → logo oficial Diamond RP
- `res/drawable/bg_main.xml` → imagem de fundo da tela principal
- `res/drawable/bg_splash.xml` → fundo da splash

## Sobre o Gradle Wrapper

O binário `gradle-wrapper.jar` não foi incluído manualmente no projeto (é um
arquivo binário que normalmente é gerado pelo Android Studio ou pelo Gradle
instalado localmente). O workflow do GitHub Actions já resolve isso sozinho
(passo "Generate Gradle Wrapper"), então **compilar pelo GitHub funciona sem
nenhuma etapa extra**. Se um dia quiser abrir no Android Studio, ele mesmo
gera o wrapper na primeira sincronização.

## Estrutura

```
DiamondRPLauncher/
├── app/src/main/java/com/diamondrp/launcher/
│   ├── activities/   # Splash, Main, Update, Settings, About, BaseActivity
│   ├── network/      # Retrofit, ApiService, models (ServerConfig, NewsItem...)
│   ├── repository/   # ConfigRepository, ServerRepository (query UDP)
│   ├── download/      # DownloadManager (engine de download com resume/checksum/unzip)
│   ├── service/      # DownloadService (foreground service)
│   ├── storage/preferences/  # DataStore (PreferencesManager)
│   ├── ui/           # ViewModels, widgets (NewsAdapter)
│   └── utils/        # Constants, Resource<T>, ThemeManager, LocaleManager
├── app/src/main/res/ # layouts, drawables, values (cores/tema/strings, PT-BR + EN)
├── .github/workflows/ # CI: build.yml (gera wrapper + APK debug/release)
└── config.example.json
```
