// JavaScript fix for Swagger UI vertical text issue
// This script will run after the page loads to fix any remaining vertical text display issues

(function() {
    'use strict';
    
    function fixVerticalText() {
        console.log('Swagger UI Fix: Attempting to fix vertical text display...');
        
        // Find all elements that might have vertical text
        const selectors = [
            '.swagger-ui .opblock .opblock-summary-path',
            '.swagger-ui .opblock .opblock-summary-path span',
            '.swagger-ui .opblock .opblock-summary-path code',
            '.swagger-ui span[data-path]',
            '.swagger-ui .opblock-summary-path-description-wrapper',
            '.opblock-summary-path',
            '[class*="opblock-summary-path"]'
        ];
        
        selectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(el => {
                // Force horizontal text rendering
                el.style.writingMode = 'horizontal-tb';
                el.style.textOrientation = 'mixed';
                el.style.direction = 'ltr';
                el.style.unicodeBidi = 'normal';
                el.style.wordBreak = 'normal';
                el.style.whiteSpace = 'nowrap';
                el.style.display = 'inline';
                el.style.flexDirection = 'row';
                el.style.flexWrap = 'nowrap';
                
                // Fix any child elements
                const children = el.querySelectorAll('*');
                children.forEach(child => {
                    child.style.writingMode = 'horizontal-tb';
                    child.style.textOrientation = 'mixed';
                    child.style.display = 'inline';
                });
            });
        });
        
        // Fix operation summaries layout
        const summaries = document.querySelectorAll('.swagger-ui .opblock .opblock-summary');
        summaries.forEach(summary => {
            summary.style.display = 'flex';
            summary.style.flexDirection = 'row';
            summary.style.alignItems = 'center';
            summary.style.flexWrap = 'nowrap';
            summary.style.width = '100%';
        });
        
        console.log('Swagger UI Fix: Applied fixes to', selectors.length, 'selector groups');
    }
    
    // Run the fix when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', fixVerticalText);
    } else {
        fixVerticalText();
    }
    
    // Also run after a delay to catch dynamically loaded content
    setTimeout(fixVerticalText, 1000);
    setTimeout(fixVerticalText, 3000);
    setTimeout(fixVerticalText, 5000);
    
    // Watch for changes and reapply fixes
    if (window.MutationObserver) {
        const observer = new MutationObserver(function(mutations) {
            let shouldFix = false;
            mutations.forEach(function(mutation) {
                if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                    shouldFix = true;
                }
            });
            if (shouldFix) {
                setTimeout(fixVerticalText, 100);
            }
        });
        
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }
    
    console.log('Swagger UI vertical text fix script loaded');
})();
