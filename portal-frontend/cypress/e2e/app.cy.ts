describe('Portail Intranet E2E', () => {
  it('should display login page', () => {
    cy.visit('/')
    cy.url().should('include', '/login')
    cy.contains('Portail Intranet')
    cy.get('input[type="text"]').should('exist')
    cy.get('input[type="password"]').should('exist')
  })

  it('should login and show dashboard', () => {
    cy.visit('/login')
    cy.get('input[type="text"]').type('admin')
    cy.get('input[type="password"]').type('admin123')
    cy.get('button[type="submit"]').click()
    cy.url().should('not.include', '/login')
    cy.contains('Bienvenue')
  })
})
