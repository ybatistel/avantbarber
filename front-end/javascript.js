const API_BASE_URL = 'http://localhost:8080';

// TODO: substituir pelo número real da barbearia (formato: 55DDDNÚMERO, só dígitos)
const WHATSAPP_NUMERO = '5500000000000';
const WHATSAPP_MENSAGEM = 'Olá! Gostaria de agendar um horário na Avant Barber.';

function montarLinkWhatsapp() {
    const link = `https://wa.me/${WHATSAPP_NUMERO}?text=${encodeURIComponent(WHATSAPP_MENSAGEM)}`;
    document.querySelectorAll('#whatsapp-link, #whatsapp-link-hero').forEach((el) => {
        el.href = link;
    });
}

function formatarPreco(preco) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(preco);
}

async function carregarServicos() {
    const lista = document.getElementById('lista-servicos');
    try {
        const resposta = await fetch(`${API_BASE_URL}/servicos-desejados/publico`);
        if (!resposta.ok) throw new Error('Falha ao buscar serviços');
        const servicos = await resposta.json();

        if (servicos.length === 0) {
            lista.innerHTML = '<li>Nenhum serviço cadastrado no momento.</li>';
            return;
        }

        lista.innerHTML = servicos.map((servico) => `
            <li>
                <span class="servico-nome">${servico.nome}</span>
                <span class="servico-preco">${formatarPreco(servico.preco)}</span>
            </li>
        `).join('');
    } catch (erro) {
        lista.innerHTML = '<li>Não foi possível carregar os serviços agora.</li>';
    }
}

async function carregarBarbeiros() {
    const lista = document.getElementById('lista-barbeiros');
    try {
        const resposta = await fetch(`${API_BASE_URL}/barbeiros/publico`);
        if (!resposta.ok) throw new Error('Falha ao buscar barbeiros');
        const barbeiros = await resposta.json();

        if (barbeiros.length === 0) {
            lista.innerHTML = '<li>Nenhum barbeiro cadastrado no momento.</li>';
            return;
        }

        lista.innerHTML = barbeiros.map((barbeiro) => `
            <li>
                <span class="barbeiro-nome">${barbeiro.nome}</span>
            </li>
        `).join('');
    } catch (erro) {
        lista.innerHTML = '<li>Não foi possível carregar a equipe agora.</li>';
    }
}

document.getElementById('ano').textContent = new Date().getFullYear();
montarLinkWhatsapp();
carregarServicos();
carregarBarbeiros();
