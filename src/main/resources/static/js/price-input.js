(() => {
  "use strict";

  const formatter = new Intl.NumberFormat("ko-KR", {
    maximumFractionDigits: 0
  });

  const formatPrice = (input) => {
    const digits = input.value.replace(/[^\d]/g, "").replace(/^0+(?=\d)/, "");
    input.value = digits === "" ? "" : formatter.format(Number(digits));
  };

  document.querySelectorAll("[data-price-input]").forEach((input) => {
    formatPrice(input);
    input.addEventListener("input", () => formatPrice(input));
  });
})();
