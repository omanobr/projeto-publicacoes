import { useState, useEffect } from 'react';

function ScrollToTopButton() {
  const [isVisible, setIsVisible] = useState(false);

  // Função para verificar a posição do scroll e mostrar/ocultar o botão
  const toggleVisibility = () => {
    if (window.pageYOffset > 300) { // O botão aparece após 300px de scroll
      setIsVisible(true);
    } else {
      setIsVisible(false);
    }
  };

  // Função para rolar a página suavemente para o topo
  const scrollToTop = () => {
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  };

  useEffect(() => {
    // Adiciona um listener de scroll quando o componente é montado
    window.addEventListener('scroll', toggleVisibility);

    // Remove o listener quando o componente é desmontado para evitar memory leaks
    return () => {
      window.removeEventListener('scroll', toggleVisibility);
    };
  }, []);

  return (
    <div className="scroll-to-top">
      {isVisible && (
        <button onClick={scrollToTop} className="scroll-to-top-btn" title="Voltar ao topo">
          &#8679; {/* Seta para cima em unicode */}
        </button>
      )}
    </div>
  );
}

export default ScrollToTopButton;
