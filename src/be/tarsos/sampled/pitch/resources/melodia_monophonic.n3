@prefix xsd:      <http://www.w3.org/2001/XMLSchema#> .
@prefix vamp:     <http://purl.org/ontology/vamp/> .
@prefix :         <#> .

:transform a vamp:Transform ;
    vamp:plugin <http://vamp-plugins.org/rdf/plugins/mtg-melodia#melodia> ;
    vamp:step_size "128"^^xsd:int ; 
    vamp:block_size "2048"^^xsd:int ; 
    vamp:program """Polyphonic""" ;
    vamp:parameter_binding [
        vamp:parameter [ vamp:identifier "maxfqr" ] ;
        vamp:value "1760"^^xsd:float ;
    ] ;
    vamp:parameter_binding [
        vamp:parameter [ vamp:identifier "minfqr" ] ;
        vamp:value "55"^^xsd:float ;
    ] ;
    vamp:parameter_binding [
        vamp:parameter [ vamp:identifier "minpeaksalience" ] ;
        vamp:value "0"^^xsd:float ;
    ] ;
    vamp:parameter_binding [
        vamp:parameter [ vamp:identifier "voicing" ] ;
        vamp:value "0.2"^^xsd:float ;
    ] ;
    vamp:output <http://vamp-plugins.org/rdf/plugins/mtg-melodia#melodia_output_melody> .
