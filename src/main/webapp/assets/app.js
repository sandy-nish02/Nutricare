document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.querySelector('.nav-toggle');
    const nav = document.querySelector('.main-nav');
    if (toggle && nav) {
        toggle.addEventListener('click', () => {
            const open = nav.classList.toggle('is-open');
            toggle.setAttribute('aria-expanded', String(open));
        });
    }
    document.querySelectorAll('form[data-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (!window.confirm(form.dataset.confirm)) event.preventDefault();
        });
    });
    document.querySelectorAll('.notice').forEach((notice) => {
        window.setTimeout(() => notice.classList.add('notice-fade'), 4500);
    });

    // Appointment pickers start 30 minutes from now (the server enforces the same rule).
    const soonest = new Date(Date.now() + 30 * 60 * 1000 - new Date().getTimezoneOffset() * 60 * 1000);
    document.querySelectorAll('input[data-future]').forEach((input) => {
        input.min = soonest.toISOString().slice(0, 16);
    });

    // Warn about oversized payment slips before the upload starts.
    document.querySelectorAll('input[type=file][data-max-bytes]').forEach((input) => {
        input.addEventListener('change', () => {
            const file = input.files[0];
            const tooLarge = file && file.size > Number(input.dataset.maxBytes);
            input.setCustomValidity(tooLarge ? 'Payment slips must be 2 MB or smaller.' : '');
            if (tooLarge) input.reportValidity();
        });
    });
});
