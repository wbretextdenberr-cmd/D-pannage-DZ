/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        // ألوان العلم الجزائري
        dz: {
          green: '#006233',
          white: '#FFFFFF',
          red:   '#D21034'
        },
        // الخلفية الداكنة
        base:    '#0B1324',
        surface: '#172238',
        border:  '#26385C',
        // الأساسي والمميز
        primary: '#2563EB',
        accent:  '#F59E0B',
        danger:  '#DC2626',
        success: '#16A34A',
        // النصوص
        ink:     '#E6EDF7',
        muted:   '#94A3B8'
      },
      fontFamily: {
        sans:    ['Cairo', 'Inter', 'sans-serif'],
        display: ['Cairo', 'Inter', 'sans-serif']
      },
      boxShadow: {
        glow:  '0 0 20px rgba(37,99,235,0.45)',
        red:   '0 0 20px rgba(220,38,38,0.55)',
        green: '0 0 20px rgba(22,163,74,0.55)'
      }
    }
  },
  plugins: []
}
