// === SISTEMA IMPOSTAZIONI CHESSVERSE ===

// Impostazioni di default
const defaultSettings = {
    // Impostazioni frecce e evidenziature
    arrowColor: '#8b4d8b',
    arrowThickness: 12,
    arrowOpacity: 80,
    highlightColor: '#8b4d8b', // Default purple to match bot page
    highlightOpacity: 85, // Opacità leggermente più alta
    // Impostazioni gioco
    showLegalMoves: true,
    enablePremoves: true,
    confirmMoves: false,
    moveAnimation: 'normal',
    defaultTime: '10+0',
    boardSize: 100, // Disabilitato: rimane fisso
    boardTheme: 'brown',
    // Piece style and background customization
    pieceStyle: 'classic', // classic | pixel | outline | large | neo
    backgroundColor: '#312e2b',
    backgroundImage: null,
    coordinatesPosition: 'inside', // 'inside', 'outside', 'none'
    highlightLastMove: true,
    soundsEnabled: true,
    masterVolume: 70,
    moveSounds: true,
    captureSounds: true
};

// Impostazioni utente correnti
let userSettings = {...defaultSettings};

// === FUNZIONI CORE ===

function initSettingsSystem() {
    console.log('Inizializzazione sistema impostazioni...');
    loadSettings();
    loadSettingsUI();
    setupSettingsControls();
}

function loadSettings() {
    const saved = localStorage.getItem('chessverse_settings');
    if (saved) {
        try {
            userSettings = {...defaultSettings, ...JSON.parse(saved)};
        } catch (e) {
            console.warn('Errore nel caricamento impostazioni, uso default');
            userSettings = {...defaultSettings};
        }
    }
    applySettings();
}

function saveSettings() {
    localStorage.setItem('chessverse_settings', JSON.stringify(userSettings));
}

function applySettings() {
    
    // Applica dimensione scacchiera (evita transform per non disallineare canvas frecce)
    const chessboard = document.getElementById('chessboard');
    if (chessboard) {
        const baseSize = 560; // px base (coerente con CSS --board-size)
        const newSize = Math.round(baseSize * (userSettings.boardSize / 100));
        chessboard.style.width = newSize + 'px';
        chessboard.style.height = newSize + 'px';
        chessboard.style.removeProperty('transform');
        chessboard.style.removeProperty('transform-origin');
        // Notifica altri script (arrow canvas) che la scacchiera è cambiata
        window.dispatchEvent(new CustomEvent('chessboard:resized', {detail:{size:newSize}}));
    }
    
    // Applica tema scacchiera
    applyBoardTheme(userSettings.boardTheme);

    // Applica stile pezzi
    if (typeof applyPieceStyle === 'function') applyPieceStyle(userSettings.pieceStyle);

    // Applica background (colore o immagine)
    if (typeof applyBoardBackground === 'function') applyBoardBackground();
    
    // Applica impostazioni evidenziature
    if (typeof applyHighlightSettings === 'function') {
        applyHighlightSettings();
    } else {
        // Se applyHighlightSettings non è disponibile, applica direttamente
        applyHighlightSettingsDirectly();
    }
}

// Funzione per applicare le impostazioni delle evidenziature direttamente
function applyHighlightSettingsDirectly() {
    const highlightColor = userSettings.highlightColor || '#f7f769';
    const highlightOpacity = (userSettings.highlightOpacity || 80) / 100;
    
    // Converti hex in rgb per maggiore compatibilità
    const r = parseInt(highlightColor.slice(1, 3), 16);
    const g = parseInt(highlightColor.slice(3, 5), 16);
    const b = parseInt(highlightColor.slice(5, 7), 16);
    
    // Crea o aggiorna lo stile dinamico per le evidenziature
    let existingStyle = document.getElementById('highlight-dynamic-style');
    if (!existingStyle) {
        existingStyle = document.createElement('style');
        existingStyle.id = 'highlight-dynamic-style';
        document.head.appendChild(existingStyle);
    }
    
    // CSS con evidenziatura ad anello sottile (centro trasparente) per highlight e selected
    // Calcola luminanza per attenuare colori molto brillanti (evita effetto "tutta la scacchiera" gialla)
    const luminance = (0.299*r + 0.587*g + 0.114*b)/255; // 0 (scuro) - 1 (chiaro)
    const borderOpacity = luminance > 0.7 ? Math.min(highlightOpacity * 0.55, 0.55) : highlightOpacity;
    const innerA1 = luminance > 0.7 ? 0.08 : 0.10;
    const innerA2 = luminance > 0.7 ? 0.025 : 0.03;

    existingStyle.textContent = `
        /* Evidenziatura casella (ring) */
        .chessboard .square.highlight::before,
        .chessboard .square.selected::before {
            content: '';
            position: absolute;
            inset: 2px;
            border: 3px solid rgba(${r}, ${g}, ${b}, ${borderOpacity});
            border-radius: 6px;
            box-shadow: 0 0 4px 1px rgba(${r}, ${g}, ${b}, ${Math.min(borderOpacity * 0.9,1)});
            background: radial-gradient(circle at center, rgba(${r}, ${g}, ${b}, ${innerA1}) 0%, rgba(${r}, ${g}, ${b}, ${innerA2}) 38%, rgba(${r}, ${g}, ${b}, 0) 60%);
            pointer-events: none;
            z-index: 14;
        }

        /* Mosse possibili - cerchio piccolo (vuote) */
        ${userSettings.showLegalMoves ? '.chessboard .square.possible-move::after' : ''} {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 22%;
            height: 22%;
            border-radius: 50%;
            background: rgba(120, 105, 140, 0.55);
            transform: translate(-50%, -50%);
            z-index: 15;
            pointer-events: none;
            border: 2px solid rgba(120, 105, 140, 0.75);
        }

        /* Catture possibili (occupate) - anello */
        ${userSettings.showLegalMoves ? '.chessboard .square.possible-move.occupied::after' : ''} {
            width: 82%;
            height: 82%;
            background: rgba(120, 105, 140, 0.10);
            border: 5px solid rgba(120, 105, 140, 0.75);
            border-radius: 50%;
            box-shadow: 0 0 4px rgba(120, 105, 140, 0.6);
        }

        /* Premove destinazione */
        ${userSettings.enablePremoves ? '.chessboard .square.premove-target::before' : ''} {
            content: '';
            position: absolute;
            inset: 2px;
            border: 3px dashed rgba(140, 120, 165, 0.8);
            border-radius: 6px;
            pointer-events: none;
            z-index: 18;
        }

        /* Premove origine */
        ${userSettings.enablePremoves ? '.chessboard .square.premove-origin::after' : ''} {
            content: '';
            position: absolute;
            inset: 0;
            background: rgba(140, 120, 165, 0.15);
            pointer-events: none;
            z-index: 12;
        }
    `;
    
    // Log only when explicit debug flag is enabled to avoid console spam
    if (window.__chessverse_debug) {
        console.log('Highlight settings applied directly:', {
            color: highlightColor,
            opacity: highlightOpacity,
            rgb: `rgb(${r}, ${g}, ${b})`
        });
    }
}

function applyBoardTheme(theme) {
    const chessboard = document.getElementById('chessboard');
    if (!chessboard) return;
    
    // Rimuovi classi tema esistenti
    chessboard.classList.remove('theme-brown', 'theme-green', 'theme-blue', 'theme-purple', 'theme-gray');
    
    // Aggiungi nuova classe tema
    chessboard.classList.add('theme-' + theme);
    
    // Definisci i colori per ogni tema
    const themes = {
        brown: { light: '#f0d9b5', dark: '#b58863' },
        green: { light: '#eeeed2', dark: '#769656' },
        blue: { light: '#dee3e6', dark: '#8ca2ad' },
        purple: { light: '#e8e9f7', dark: '#9f90c4' },
        gray: { light: '#e5e5e5', dark: '#999999' }
    };
    
    const colors = themes[theme] || themes.brown;
    document.documentElement.style.setProperty('--board-light', colors.light);
    document.documentElement.style.setProperty('--board-dark', colors.dark);
    
    // Aggiorna le evidenziature per riflettere il nuovo tema
    if (typeof applyHighlightSettingsDirectly === 'function') {
        applyHighlightSettingsDirectly();
    }
    if (typeof updateExistingHighlights === 'function') {
        updateExistingHighlights();
    }
}

// === FUNZIONI MODAL ===

function openSettings() {
    const overlay = document.getElementById('settingsOverlay');
    if (overlay) {
        overlay.style.display = 'block';
        loadSettingsUI();
    }
}

function closeSettings() {
    const overlay = document.getElementById('settingsOverlay');
    if (overlay) {
        overlay.style.display = 'none';
    }
}

function loadSettingsUI() {
    // Carica valori nei controlli UI
    const elements = {
        arrowColor: userSettings.arrowColor,
        arrowThickness: userSettings.arrowThickness,
        arrowOpacity: userSettings.arrowOpacity,
        highlightColor: userSettings.highlightColor,
        highlightOpacity: userSettings.highlightOpacity,
        boardSize: userSettings.boardSize,
        boardTheme: userSettings.boardTheme,
        defaultTime: userSettings.defaultTime,
        moveAnimation: userSettings.moveAnimation,
        masterVolume: userSettings.masterVolume
    };

    // New elements: piece style and background
    elements.pieceStyle = userSettings.pieceStyle;
    elements.backgroundColor = userSettings.backgroundColor;
    elements.backgroundImage = userSettings.backgroundImage || '';

    // Imposta valori input
    for (const [id, value] of Object.entries(elements)) {
        const element = document.getElementById(id);
        if (element) {
            element.value = value;
        }
    }

    // file input cannot set .value to data URL; if backgroundImage exists, create a small preview or note
    if (userSettings.backgroundImage) {
        const preview = document.getElementById('backgroundImagePreview');
        if (preview) preview.src = userSettings.backgroundImage;
    }

    // Aggiorna i display dei valori
    updateSliderValue('arrowThickness', userSettings.arrowThickness, 'px');
    updateSliderValue('arrowOpacity', userSettings.arrowOpacity, '%');
    updateSliderValue('highlightOpacity', userSettings.highlightOpacity, '%');
    updateSliderValue('boardSize', userSettings.boardSize, '%');
    updateSliderValue('masterVolume', userSettings.masterVolume, '%');
    
    // Imposta toggle
    const toggles = [
        'showLegalMoves', 'enablePremoves', 'confirmMoves',
        'showCoordinates', 'highlightLastMove', 'soundsEnabled', 'moveSounds', 'captureSounds'
    ];
    
    toggles.forEach(toggleId => {
        setToggle(toggleId, userSettings[toggleId]);
    });
}

function updateSliderValue(sliderId, value, unit) {
    const valueElement = document.getElementById(sliderId + 'Value');
    if (valueElement) {
        valueElement.textContent = value + unit;
    }
}

function setToggle(id, active) {
    const toggle = document.getElementById(id);
    if (toggle) {
        if (active) {
            toggle.classList.add('active');
        } else {
            toggle.classList.remove('active');
        }
    }
}

// === SETUP EVENT LISTENERS ===

function initSettings() {
    console.debug('[settings] initSettings called');
    loadSettings();
    
    // Event listeners per le tab
    document.querySelectorAll('.settings-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            const targetTab = this.dataset.tab;
            
            // Aggiorna tab attive
            document.querySelectorAll('.settings-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.settings-panel').forEach(p => p.classList.remove('active'));
            
            this.classList.add('active');
            const panel = document.getElementById(targetTab + '-panel');
            if (panel) {
                panel.classList.add('active');
            }
        });
    });

    // Event listeners per i controlli
    setupSettingsControls();
    
    // Chiudi modal cliccando fuori
    const overlay = document.getElementById('settingsOverlay');
    if (overlay) {
        overlay.addEventListener('click', function(e) {
            if (e.target === this) {
                closeSettings();
            }
        });
    }
}

function setupSettingsControls() {
    // Color picker per frecce
    setupControl('arrowColor', 'change', function() {
        userSettings.arrowColor = this.value;
        saveSettings();
        applySettings();
    });

    // Slider spessore frecce
    setupControl('arrowThickness', 'input', function() {
        userSettings.arrowThickness = parseInt(this.value);
        updateSliderValue('arrowThickness', this.value, 'px');
        saveSettings();
        applySettings();
    });

    // Slider opacità frecce
    setupControl('arrowOpacity', 'input', function() {
        userSettings.arrowOpacity = parseInt(this.value);
        updateSliderValue('arrowOpacity', this.value, '%');
        saveSettings();
        applySettings();
    });

    // Color picker evidenziature
    setupControl('highlightColor', 'change', function() {
        userSettings.highlightColor = this.value;
        console.log('Highlight color changed to:', this.value);
        saveSettings();
        applySettings();
        // Forza l'aggiornamento immediato delle evidenziature esistenti
        updateExistingHighlights();
    });

    // Slider opacità evidenziature
    setupControl('highlightOpacity', 'input', function() {
        userSettings.highlightOpacity = parseInt(this.value);
        updateSliderValue('highlightOpacity', this.value, '%');
        saveSettings();
        applySettings();
        // Forza l'aggiornamento immediato delle evidenziature esistenti
        updateExistingHighlights();
    });

    // Slider dimensione scacchiera disabilitato (nessuna azione)

    // Slider volume
    setupControl('masterVolume', 'input', function() {
        userSettings.masterVolume = parseInt(this.value);
        updateSliderValue('masterVolume', this.value, '%');
        saveSettings();
    });

    // Dropdown tema scacchiera
    setupControl('boardTheme', 'change', function() {
        userSettings.boardTheme = this.value;
        saveSettings();
        applySettings();
    });

    // Piece style dropdown
    setupControl('pieceStyle', 'change', function() {
        userSettings.pieceStyle = this.value;
        saveSettings();
        applyPieceStyle(userSettings.pieceStyle);
    });

    // Background color picker
    setupControl('backgroundColor', 'change', function() {
        userSettings.backgroundColor = this.value;
        saveSettings();
        applyBoardBackground();
    });

    // Background image file input (expects a data URL)
    setupControl('backgroundImage', 'change', function() {
        const file = this.files && this.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = function(e) {
            userSettings.backgroundImage = e.target.result;
            saveSettings();
            applyBoardBackground();
        };
        reader.readAsDataURL(file);
    });

    // Altri dropdown
    setupControl('defaultTime', 'change', function() {
        userSettings.defaultTime = this.value;
        saveSettings();
    });

    setupControl('moveAnimation', 'change', function() {
        userSettings.moveAnimation = this.value;
        saveSettings();
    });

    // Toggle buttons
    const toggles = [
        'showLegalMoves', 'enablePremoves', 'confirmMoves',
        'showCoordinates', 'highlightLastMove', 'soundsEnabled', 'moveSounds', 'captureSounds'
    ];
    
    toggles.forEach(toggleId => {
        setupToggle(toggleId);
    });
}

// Apply piece style by switching CSS or classes
function applyPieceStyle(style) {
    const board = document.getElementById('chessboard');
    if (!board) return;
    // Remove known piece style classes
    board.classList.remove('pieces-classic','pieces-pixel','pieces-outline','pieces-large','pieces-neo');
    if (style === 'classic') board.classList.add('pieces-classic');
    else if (style === 'pixel') board.classList.add('pieces-pixel');
    else if (style === 'outline') board.classList.add('pieces-outline');
    else if (style === 'large') board.classList.add('pieces-large');
    else if (style === 'neo') board.classList.add('pieces-neo');
    // Inject minimal CSS for styles if not present
    let css = document.getElementById('piece-style-dynamic');
    if (!css) { css = document.createElement('style'); css.id = 'piece-style-dynamic'; document.head.appendChild(css); }
    css.textContent = `
    /* pixel: sharp, pixel-art friendly rendering */
    .pieces-pixel .piece { image-rendering: pixelated; shape-rendering: crispEdges; filter: none; }
    /* outline: stroke-only appearance */
    .pieces-outline .piece { background-image: none !important; border: 2px solid rgba(255,255,255,0.12); box-shadow: inset 0 0 0 2px rgba(0,0,0,0.35); }
    /* large: bigger visual scale */
    .pieces-large .piece { width: 95% !important; height: 95% !important; }
    /* neo: new custom piece set served from /pieces/neo */
    .pieces-neo .piece.white.king { background-image: url('/pieces/neo/white/king.svg'); }
    .pieces-neo .piece.white.queen { background-image: url('/pieces/neo/white/queen.svg'); }
    .pieces-neo .piece.white.rook { background-image: url('/pieces/neo/white/rook.svg'); }
    .pieces-neo .piece.white.bishop { background-image: url('/pieces/neo/white/bishop.svg'); }
    .pieces-neo .piece.white.knight { background-image: url('/pieces/neo/white/knight.svg'); }
    .pieces-neo .piece.white.pawn { background-image: url('/pieces/neo/white/pawn.svg'); }
    .pieces-neo .piece.black.king { background-image: url('/pieces/neo/black/king.svg'); }
    .pieces-neo .piece.black.queen { background-image: url('/pieces/neo/black/queen.svg'); }
    .pieces-neo .piece.black.rook { background-image: url('/pieces/neo/black/rook.svg'); }
    .pieces-neo .piece.black.bishop { background-image: url('/pieces/neo/black/bishop.svg'); }
    .pieces-neo .piece.black.knight { background-image: url('/pieces/neo/black/knight.svg'); }
    .pieces-neo .piece.black.pawn { background-image: url('/pieces/neo/black/pawn.svg'); }
    `;
}

function applyBoardBackground(){
    const root = document.documentElement;
    const board = document.getElementById('chessboard');
    if (!board) return;
    const bg = userSettings.backgroundImage;
    const color = userSettings.backgroundColor || '#312e2b';
    if (bg) {
        root.style.setProperty('--app-background', `url(${bg})`);
        board.style.backgroundImage = `url(${bg})`;
        board.style.backgroundSize = 'cover';
        board.style.backgroundRepeat = 'no-repeat';
    } else {
        root.style.setProperty('--app-background', color);
        board.style.removeProperty('background-image');
        // restore theme gradient via applyBoardTheme
        applyBoardTheme(userSettings.boardTheme);
    }
}

function setupControl(id, event, handler) {
    const element = document.getElementById(id);
    if (element) {
        element.addEventListener(event, handler);
    }
}

function setupToggle(settingName) {
    const toggle = document.getElementById(settingName);
    if (toggle) {
        console.debug('[settings] setupToggle registering for', settingName, 'element=', toggle);
        toggle.addEventListener('click', function() {
            const isActive = this.classList.contains('active');
            const newVal = !isActive;
            console.debug('[settings] toggle clicked', settingName, 'currentActive=', isActive, 'newVal=', newVal);
            if (isActive) {
                this.classList.remove('active');
                userSettings[settingName] = false;
            } else {
                this.classList.add('active');
                userSettings[settingName] = true;
            }
            saveSettings();
            applySettings();
        });
    }
}

// === FUNZIONI UTILITY ===

// Funzione per ottenere un setting specifico
function getSetting(key) {
    return userSettings[key];
}

// Funzione per impostare un setting
function setSetting(key, value) {
    userSettings[key] = value;
    saveSettings();
    applySettings();
}

// Rendi le funzioni disponibili globalmente
window.getSetting = getSetting;
window.setSetting = setSetting;

// Funzione per resettare alle impostazioni di default
function resetToDefaults() {
    if (confirm('Sei sicuro di voler resettare tutte le impostazioni ai valori di default?')) {
        userSettings = {...defaultSettings};
        saveSettings();
        applySettings();
        loadSettingsUI();
    }
}

// === INTEGRAZIONE CON DRAWARRAY ===

// Wrapper per migliorare la funzione drawArrow esistente
function enhanceDrawArrow(originalDrawArrow) {
    return function(ctx, from, to, color, canvas) {
        // Salva il contesto
        ctx.save();
        
        // Usa il colore dalle impostazioni se è il colore principale
        let arrowColor;
        if (color === 'red' || !color) {
            arrowColor = getSetting('arrowColor');
        } else {
            const colors = {
                red: '#e74c3c', green: '#27ae60', blue: '#3498db',
                yellow: '#f1c40f', orange: '#e67e22', purple: '#9b59b6'
            };
            arrowColor = colors[color] || getSetting('arrowColor');
        }
        
        // Applica impostazioni
        const opacity = getSetting('arrowOpacity') / 100;
        const thickness = getSetting('arrowThickness');
        
        ctx.globalAlpha = opacity;
        ctx.strokeStyle = arrowColor;
        ctx.fillStyle = arrowColor;
        ctx.lineWidth = thickness;
        ctx.lineCap = 'round';
        
        // Calcola coordinate
        const squareSize = canvas.width / 8;
        const [fromRow, fromCol] = squareToPosition ? squareToPosition(from) : [0, 0];
        const [toRow, toCol] = squareToPosition ? squareToPosition(to) : [0, 0];
        
        const fromX = (fromCol + 0.5) * squareSize;
        const fromY = (fromRow + 0.5) * squareSize;
        const toX = (toCol + 0.5) * squareSize;
        const toY = (toRow + 0.5) * squareSize;
        
        // Calcola dimensioni freccia proporzionate al thickness
        const arrowLength = Math.max(20, thickness * 2.5);
        const shortenDistance = arrowLength * 0.7;
        
        const angle = Math.atan2(toY - fromY, toX - fromX);
        const adjustedToX = toX - shortenDistance * Math.cos(angle);
        const adjustedToY = toY - shortenDistance * Math.sin(angle);
        
        // Disegna linea
        ctx.beginPath();
        ctx.moveTo(fromX, fromY);
        ctx.lineTo(adjustedToX, adjustedToY);
        ctx.stroke();
        
        // Disegna punta freccia
        const arrowAngle = Math.PI / 4.5;
        ctx.beginPath();
        ctx.moveTo(toX, toY);
        ctx.lineTo(
            toX - arrowLength * Math.cos(angle - arrowAngle),
            toY - arrowLength * Math.sin(angle - arrowAngle)
        );
        ctx.lineTo(
            toX - arrowLength * Math.cos(angle + arrowAngle),
            toY - arrowLength * Math.sin(angle + arrowAngle)
        );
        ctx.closePath();
        ctx.fill();
        
        // Ripristina il contesto
        ctx.restore();
    };
}

// === FUNZIONI AGGIUNTIVE PER EVIDENZIATURE ===

// Funzione per aggiornare le evidenziature esistenti
function updateExistingHighlights() {
    // Riapplica gli stili alle caselle evidenziate esistenti
    const highlightedSquares = document.querySelectorAll('.square.highlight, .square.selected, .square.possible-move');
    if (highlightedSquares.length > 0) {
        console.log('Updating', highlightedSquares.length, 'existing highlights');
        // Forza il ricalcolo degli stili CSS
        highlightedSquares.forEach(square => {
            square.style.display = 'none';
            square.offsetHeight; // Trigger reflow
            square.style.display = '';
        });
    }
}

// Rende la funzione disponibile globalmente
window.applyHighlightSettingsDirectly = applyHighlightSettingsDirectly;
window.updateExistingHighlights = updateExistingHighlights;

// === AUTO-INIZIALIZZAZIONE ===

// Inizializza quando il DOM è pronto
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initSettings);
} else {
    initSettings();
}
