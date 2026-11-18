// Dev-only check script. Copied into the run folder's kubejs/server_scripts/ by runGameTestServer or runClient
// when -PwithKubeJS is set; KubeJsTests reads the result. KubeJS 6 (1.20.1) has no data-driven schemas, so this
// uses replaceInput, a remove by id, a plain shaped recipe and a tag add.
ServerEvents.recipes(event => {
  event.replaceInput({ id: 'gt_spartan:shaped/gtceu/steel_longsword' }, '#forge:rods/steel', 'minecraft:blaze_rod')
  event.remove({ id: 'gt_spartan:shaped/gtceu/steel_dagger' })
  event.shaped('gtceu:steel_dagger', [' P', 'S '], { P: '#forge:plates/steel', S: 'minecraft:stick' })
    .id('kubejs:gtstest_steel_dagger')
  console.info('GTSTEST script ran')
})
ServerEvents.tags('item', event => {
  event.add('kubejs_test:marker', 'gtceu:steel_halberd')
})
