# Como Rodar a Aventura da Helena na Android TV

## Opção 1: Baixar o APK pronto (mais fácil)

1. No GitHub, clique em **Actions** → último build bem-sucedido
2. Baixe o arquivo `aventura-helena-debug.apk`
3. Copie o APK para um pendrive
4. Na Android TV, instale via gerenciador de arquivos (precisa habilitar "fontes desconhecidas" nas configurações)

---

## Opção 2: Compilar pelo Android Studio

### O que você precisa
- Android Studio (gratuito em developer.android.com/studio)
- Java 11 ou superior

### Passos
1. Abra o Android Studio
2. Vá em **File → Open** e selecione a pasta `aventura-helena-android`
3. Aguarde o Gradle sincronizar
4. Clique em **Build → Build APK(s)**
5. O APK será gerado em: `app/build/outputs/apk/debug/app-debug.apk`

---

## Como instalar na TV

### Via ADB (Wi-Fi)
1. Na TV, vá em **Configurações → Sobre → Número da versão** (clique 7x para ativar Opções do desenvolvedor)
2. Em **Opções do desenvolvedor**, ative **Depuração ADB**
3. Veja o IP da TV em **Configurações → Rede**
4. No computador, execute:
   ```
   adb connect IP_DA_TV:5555
   adb install app-debug.apk
   ```

### Via pendrive
1. Copie o APK para um pendrive
2. Na TV, use um gerenciador de arquivos (ex: ES File Explorer) para abrir e instalar

---

## Controles

| Botão do controle | Ação |
|---|---|
| Setas direcionais | Navegar entre opções |
| OK / Confirmar | Selecionar / Virar carta / Responder |
| Voltar | Voltar ao hub |

---

## Estrutura do Jogo

**Hub Principal**
- Perfil da Helena com XP, Nível e Stats
- Botões para os 3 mini-jogos

**Jogo da Memória**
- 24 cartas (12 pares de emojis)
- Grade 4x6

**Complete a Palavra**
- 15 perguntas com palavras embaralhadas
- 4 opções de resposta cada

**Tarefas de Casa**
- 10 tarefas diárias
- Resetam todo dia

**Batalha Final** (desbloqueada quando tudo for concluído)
- Fase 1: Memória com 3 pares (6 cartas)
- Fase 2: 3 perguntas de palavras
- Sistema de HP para Helena e o Bruxo
